/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.admin

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import jakarta.transaction.Transactional.TxType.REQUIRED
import jakarta.transaction.Transactional.TxType.SUPPORTS
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.zrc.model.ZaakListParameters
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.extensions.isServicenormAvailable
import nl.info.client.zgw.ztc.model.generated.ZaakType
import nl.info.zac.admin.model.ProcessEngine
import nl.info.zac.admin.model.ZaaktypeConfiguration
import nl.info.zac.exception.ErrorCode.ERROR_CODE_PROCESS_DEFINITION_CANNOT_CHANGE
import nl.info.zac.exception.ErrorCode.ERROR_CODE_PRODUCTAANVRAAGTYPE_ALREADY_IN_USE
import nl.info.zac.exception.InputValidationFailedException
import nl.info.zac.smartdocuments.SmartDocumentsTemplatesService
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.net.URI
import java.util.UUID
import java.util.logging.Logger

/**
 * Stores zaaktype configurations, whatever process engine they are bound to.
 */
@ApplicationScoped
@Transactional(SUPPORTS)
@NoArgConstructor
@AllOpen
class ZaaktypeConfigurationBeheerService @Inject constructor(
    private val zaaktypeConfigurationRepository: ZaaktypeConfigurationRepository,
    private val zaaktypeConfigurationService: ZaaktypeConfigurationService,
    private val ztcClientService: ZtcClientService,
    private val zrcClientService: ZrcClientService,
    private val smartDocumentsTemplatesService: SmartDocumentsTemplatesService,
    private val zaaktypeConfigurationVersioning: ZaaktypeConfigurationVersioning
) {
    companion object {
        private val LOG = Logger.getLogger(ZaaktypeConfigurationBeheerService::class.java.name)
    }

    fun findConfiguration(zaaktypeUuid: UUID): ZaaktypeConfiguration? =
        zaaktypeConfigurationRepository.findByZaaktypeUuid(zaaktypeUuid)

    /**
     * Stores the configuration. When the zaaktype version already has a configuration, that configuration is
     * updated, whatever id the given configuration carries.
     */
    @Transactional(REQUIRED)
    fun storeConfiguration(zaaktypeConfiguration: ZaaktypeConfiguration): ZaaktypeConfiguration {
        zaaktypeConfiguration.id = zaaktypeConfigurationRepository.findByZaaktypeUuid(
            zaaktypeConfiguration.zaaktypeUuid
        )?.id
        return zaaktypeConfigurationRepository.store(zaaktypeConfiguration).also {
            zaaktypeConfigurationService.evict(it.zaaktypeUuid)
        }
    }

    /**
     * Stores the configuration as [storeConfiguration] does, and builds [toResponse] of the stored configuration in
     * the same transaction, so that a failure while building it stores nothing.
     */
    @Transactional(REQUIRED)
    fun <T> storeConfiguration(zaaktypeConfiguration: ZaaktypeConfiguration, toResponse: (ZaaktypeConfiguration) -> T): T =
        toResponse(storeConfiguration(zaaktypeConfiguration))

    /**
     * Indicates whether a beheerder may bind the configuration to another process definition, of either engine:
     * until the first zaak of its zaaktype version exists, no zaak depends on its binding.
     */
    fun isProcessDefinitionChangeable(zaaktypeConfiguration: ZaaktypeConfiguration): Boolean =
        zaaktypeConfiguration.processBinding == null || !hasZaken(zaaktypeConfiguration.zaaktypeUuid)

    /**
     * Rejects binding the configuration of the zaaktype version to another engine or process definition when
     * [isProcessDefinitionChangeable] does not allow it.
     */
    fun checkProcessDefinitionMayChange(zaaktypeUuid: UUID, processEngine: ProcessEngine, definitionKey: String) {
        val binding = findConfiguration(zaaktypeUuid)?.processBinding ?: return
        val isSameBinding = binding.processEngine == processEngine && binding.definitionKey == definitionKey
        if (!isSameBinding && hasZaken(zaaktypeUuid)) {
            throw InputValidationFailedException(
                errorCode = ERROR_CODE_PROCESS_DEFINITION_CANNOT_CHANGE,
                message = "The process definition of the configuration of zaaktype '$zaaktypeUuid' cannot change " +
                    "from ${binding.processEngine} '${binding.definitionKey}' to $processEngine '$definitionKey'"
            )
        }
    }

    private fun hasZaken(zaaktypeUuid: UUID): Boolean =
        zrcClientService.listZakenUuids(
            ZaakListParameters().apply { zaaktype = ztcClientService.readZaaktype(zaaktypeUuid).url }
        ).count() > 0

    /**
     * Rejects a productaanvraagtype that the current configuration of a zaaktype with another omschrijving uses,
     * whatever process engine either configuration is bound to.
     */
    fun checkProductaanvraagtypeIsNotInUse(productaanvraagtype: String, zaaktypeOmschrijving: String) {
        zaaktypeConfigurationService.listCurrentConfigurationsByProductaanvraagtype(productaanvraagtype)
            .firstOrNull { it.zaaktypeOmschrijving != zaaktypeOmschrijving }
            ?.let {
                LOG.info {
                    "Productaanvraagtype '$productaanvraagtype' is already in use by zaaktype " +
                        "'${it.zaaktypeOmschrijving}' with UUID '${it.zaaktypeUuid}'"
                }
                throw InputValidationFailedException(ERROR_CODE_PRODUCTAANVRAAGTYPE_ALREADY_IN_USE)
            }
    }

    /**
     * Removes the given zaaktype version and its related types from the ZTC caches, and then creates or updates
     * the configuration of that zaaktype version, unless it is still a concept.
     */
    @Transactional(REQUIRED)
    fun updateZaaktypeConfiguration(zaaktypeUri: URI) {
        ztcClientService.invalidateZaaktype(zaaktypeUri)
        val zaaktype = ztcClientService.readZaaktype(zaaktypeUri)
        if (zaaktype.concept) {
            LOG.info { "Zaaktype '${zaaktype.omschrijving}' with UUID ${zaaktypeUri.extractUuid()} is still a concept. Ignoring" }
            return
        }
        upsertConfiguration(zaaktype)
    }

    @Transactional(REQUIRED)
    fun upsertConfiguration(zaaktype: ZaakType) {
        val zaaktypeUuid = zaaktype.url.extractUuid()
        zaaktypeConfigurationRepository.findByZaaktypeUuid(zaaktypeUuid)?.let { existingConfiguration ->
            LOG.info { "Zaaktype configuration for zaaktype with UUID $zaaktypeUuid already exists. Updating it" }
            existingConfiguration.apply {
                zaaktypeOmschrijving = zaaktype.omschrijving
                einddatumGeplandWaarschuwing = einddatumGeplandWaarschuwing.takeIf { zaaktype.isServicenormAvailable() }
            }
            storeConfiguration(existingConfiguration)
            return
        }
        val previousConfiguration = zaaktypeConfigurationRepository.findCurrentByZaaktypeOmschrijving(
            zaaktype.omschrijving
        ) ?: run {
            LOG.info { "Zaaktype '${zaaktype.omschrijving}' with UUID $zaaktypeUuid has no known configuration. Ignoring" }
            return
        }
        storeConfiguration(zaaktypeConfigurationVersioning.createNextVersion(previousConfiguration, zaaktype))
        smartDocumentsTemplatesService.copySmartDocumentsTemplateMappings(previousConfiguration.zaaktypeUuid, zaaktypeUuid)
    }
}
