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
import nl.info.zac.admin.log.ProductaanvraagtypeEvent
import nl.info.zac.admin.model.ProcessEngine
import nl.info.zac.admin.model.ZaaktypeConfiguration
import nl.info.zac.exception.ErrorCode.ERROR_CODE_BPMN_PROCESS_DEFINITION_NOT_FOUND
import nl.info.zac.exception.ErrorCode.ERROR_CODE_PROCESS_DEFINITION_CANNOT_CHANGE
import nl.info.zac.exception.ErrorCode.ERROR_CODE_PRODUCTAANVRAAGTYPE_ALREADY_IN_USE
import nl.info.zac.exception.InputValidationFailedException
import nl.info.zac.flowable.bpmn.BpmnService
import nl.info.zac.log.ZaaktypeEvent
import nl.info.zac.log.ZacLogger
import nl.info.zac.smartdocuments.SmartDocumentsTemplatesService
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.net.URI
import java.util.UUID

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
    private val zaaktypeConfigurationVersioning: ZaaktypeConfigurationVersioning,
    private val bpmnService: BpmnService
) {
    companion object {
        private val LOG = ZacLogger.getLogger(ZaaktypeConfigurationBeheerService::class)
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
     * Indicates whether a beheerder may bind the configuration to another process definition, of either engine:
     * until the first zaak of its zaaktype version exists, no zaak depends on its binding.
     */
    fun isProcessDefinitionChangeable(zaaktypeConfiguration: ZaaktypeConfiguration) =
        isProcessDefinitionChangeable(isBound = zaaktypeConfiguration.processBinding != null) {
            hasZaken(zaaktypeConfiguration.zaaktypeUuid)
        }

    /**
     * Rejects binding the configuration of the zaaktype version to another engine or process definition when its
     * zaaktype version [hasZaken].
     * Also rejects a new binding to a BPMN process definition that is not active: the first zaak would be created in
     * Open Zaak before its process fails to start, and that zaak would lock the binding.
     *
     * @return whether the process definition stays changeable once the configuration is bound to it
     */
    fun checkProcessBindingAndReadChangeability(
        zaaktypeUuid: UUID,
        processEngine: ProcessEngine,
        definitionKey: String
    ): Boolean {
        val hasZaken = hasZaken(zaaktypeUuid)
        val isProcessDefinitionStillChangeable = isProcessDefinitionChangeable(isBound = true) { hasZaken }
        val binding = findConfiguration(zaaktypeUuid)?.processBinding
        if (binding?.isBoundTo(processEngine, definitionKey) == true) return isProcessDefinitionStillChangeable
        if (binding != null && hasZaken) {
            throw InputValidationFailedException(
                errorCode = ERROR_CODE_PROCESS_DEFINITION_CANNOT_CHANGE,
                message = "The process definition of the configuration of zaaktype '$zaaktypeUuid' cannot change " +
                    "from ${binding.processEngine} '${binding.definitionKey}' to $processEngine '$definitionKey'"
            )
        }
        if (processEngine == ProcessEngine.BPMN &&
            bpmnService.findProcessDefinitionByProcessDefinitionKey(definitionKey) == null
        ) {
            throw InputValidationFailedException(
                errorCode = ERROR_CODE_BPMN_PROCESS_DEFINITION_NOT_FOUND,
                message = "The configuration of zaaktype '$zaaktypeUuid' cannot be bound to BPMN process definition " +
                    "'$definitionKey', because it has no active version"
            )
        }
        return isProcessDefinitionStillChangeable
    }

    private fun isProcessDefinitionChangeable(isBound: Boolean, hasZaken: () -> Boolean) = !isBound || !hasZaken()

    fun hasZaken(zaaktypeUuid: UUID): Boolean =
        zrcClientService.listZakenUuids(
            ZaakListParameters().apply {
                zaaktype = ztcClientService.readZaaktype(zaaktypeUuid).url
                pageSize = 1
            }
        ).count() > 0

    /**
     * Rejects a productaanvraagtype that the current configuration of a zaaktype with another omschrijving uses,
     * whatever process engine either configuration is bound to.
     */
    fun checkProductaanvraagtypeIsNotInUse(productaanvraagtype: String, zaaktypeOmschrijving: String) {
        zaaktypeConfigurationService.listCurrentConfigurationsByProductaanvraagtype(productaanvraagtype)
            .firstOrNull { it.zaaktypeOmschrijving != zaaktypeOmschrijving }
            ?.let {
                LOG.info(
                    ProductaanvraagtypeEvent(type = ProductaanvraagtypeEvent.Type.PRODUCTAANVRAAGTYPE_ALREADY_IN_USE, 
                        productaanvraagType = productaanvraagtype,
                        zaaktypeOmschrijving = it.zaaktypeOmschrijving,
                        zaaktypeUuid = it.zaaktypeUuid
                    )
                )
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
            LOG.info(
                ZaaktypeEvent(
                    type = ZaaktypeEvent.Type.CONCEPT_ZAAKTYPE_IGNORED,
                    zaaktypeOmschrijving = zaaktype.omschrijving,
                    zaaktypeUuid = zaaktypeUri.extractUuid()
                )
            )
            return
        }
        upsertConfiguration(zaaktype)
    }

    @Transactional(REQUIRED)
    fun upsertConfiguration(zaaktype: ZaakType) {
        val zaaktypeUuid = zaaktype.url.extractUuid()
        zaaktypeConfigurationRepository.findByZaaktypeUuid(zaaktypeUuid)?.let { existingConfiguration ->
            LOG.info(ZaaktypeEvent(type = ZaaktypeEvent.Type.ZAAKTYPE_CONFIGURATION_UPDATING, zaaktypeUuid = zaaktypeUuid))
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
            LOG.info(
                ZaaktypeEvent(
                    type = ZaaktypeEvent.Type.ZAAKTYPE_WITHOUT_CONFIGURATION_IGNORED,
                    zaaktypeOmschrijving = zaaktype.omschrijving,
                    zaaktypeUuid = zaaktypeUuid
                )
            )
            return
        }
        storeConfiguration(zaaktypeConfigurationVersioning.createNextVersion(previousConfiguration, zaaktype))
        smartDocumentsTemplatesService.copySmartDocumentsTemplateMappings(previousConfiguration.zaaktypeUuid, zaaktypeUuid)
    }
}
