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
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.extensions.isServicenormAvailable
import nl.info.client.zgw.ztc.model.generated.ZaakType
import nl.info.zac.admin.model.ZaaktypeConfiguration
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
     * Clears the ZTC caches for zaaktypen, roltypen, resultaattypen, statustypen and eigenschappen, and then
     * creates or updates the configuration of the given zaaktype version, unless it is still a concept.
     */
    @Transactional(REQUIRED)
    fun updateZaaktypeConfiguration(zaaktypeUri: URI) {
        ztcClientService.clearZaaktypeCache()
        ztcClientService.clearRoltypeCache()
        ztcClientService.clearResultaattypeCache()
        ztcClientService.clearStatustypeCache()
        ztcClientService.clearEigenschapCache()
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
