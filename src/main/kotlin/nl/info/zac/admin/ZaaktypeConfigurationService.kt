/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.admin

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.zac.admin.model.ZaaktypeConfiguration
import nl.info.zac.admin.model.ZaaktypeDeadlineWarningWindows
import nl.info.zac.admin.model.ZaaktypeConfiguration.Companion.CREATIEDATUM_VARIABLE_NAME
import nl.info.zac.admin.model.ZaaktypeConfiguration.Companion.ZAAKTYPE_OMSCHRIJVING_VARIABLE_NAME
import nl.info.zac.admin.model.ZaaktypeConfiguration.Companion.ZAAKTYPE_UUID_VARIABLE_NAME
import nl.info.zac.admin.model.ZaaktypeConfiguration.Companion.ZaaktypeConfigurationType
import nl.info.zac.admin.model.ZaaktypeConfiguration.Companion.ZaaktypeConfigurationType.BPMN
import nl.info.zac.admin.model.ZaaktypeConfiguration.Companion.ZaaktypeConfigurationType.CMMN
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.net.URI
import java.util.UUID
import java.util.logging.Logger

@ApplicationScoped
@Transactional
@NoArgConstructor
@AllOpen
class ZaaktypeConfigurationService @Inject constructor(
    private val entityManager: EntityManager,
    private val ztcClientService: ZtcClientService,
    private val zaaktypeCmmnConfigurationBeheerService: ZaaktypeCmmnConfigurationBeheerService,
    private val zaaktypeBpmnConfigurationBeheerService: ZaaktypeBpmnConfigurationBeheerService
) {
    companion object {
        private val LOG = Logger.getLogger(ZaaktypeConfigurationService::class.java.name)
    }

    private val beheerServicesByConfigurationType by lazy {
        mapOf<ZaaktypeConfigurationType, ZaaktypeConfigurationBeheerService>(
            CMMN to zaaktypeCmmnConfigurationBeheerService,
            BPMN to zaaktypeBpmnConfigurationBeheerService
        )
    }

    fun updateZaaktypeConfiguration(zaaktypeUri: URI) {
        ztcClientService.clearZaaktypeCache()
        ztcClientService.clearRoltypeCache()
        ztcClientService.clearResultaattypeCache()
        ztcClientService.clearStatustypeCache()
        ztcClientService.clearEigenschapCache()
        ztcClientService.readZaaktype(zaaktypeUri).let {
            if (it.concept) {
                LOG.info { "Zaaktype '${it.omschrijving}' with UUID ${zaaktypeUri.extractUuid()} is still a concept. Ignoring" }
                return
            }
            getLastCreatedConfiguration(it.omschrijving)?.let { zaaktypeConfiguration ->
                beheerServicesByConfigurationType
                    .getValue(zaaktypeConfiguration.getConfigurationType())
                    .upsertConfiguration(it)
            } ?: LOG.info {
                "Zaaktype '${it.omschrijving}' with UUID ${zaaktypeUri.extractUuid()} has no known configuration. Ignoring"
            }
        }
    }

    /**
     * Reads the ZaaktypeConfiguration for a specific zaaktype UUID.
     *
     * @param zaaktypeUUID UUID of the zaaktype (version).
     * @return ZaaktypeConfiguration for the specified zaaktype UUID or null if no configuration exists.
     */
    fun readZaaktypeConfiguration(zaaktypeUUID: UUID): ZaaktypeConfiguration? {
        val criteriaBuilder = entityManager.criteriaBuilder
        val query = criteriaBuilder.createQuery(ZaaktypeConfiguration::class.java)
        val root = query.from(ZaaktypeConfiguration::class.java)

        query.select(root)
            .where(criteriaBuilder.equal(root.get<UUID>(ZAAKTYPE_UUID_VARIABLE_NAME), zaaktypeUUID))

        return entityManager.createQuery(query).setMaxResults(1).resultList.firstOrNull()
    }

    private fun getLastCreatedConfiguration(zaaktypeDescription: String): ZaaktypeConfiguration? {
        val criteriaBuilder = entityManager.criteriaBuilder
        val query = criteriaBuilder.createQuery(ZaaktypeConfiguration::class.java)
        val root = query.from(ZaaktypeConfiguration::class.java)

        query.select(root)
            .where(criteriaBuilder.equal(root.get<UUID>(ZAAKTYPE_OMSCHRIJVING_VARIABLE_NAME), zaaktypeDescription))
            .orderBy(criteriaBuilder.desc(root.get<Any>(CREATIEDATUM_VARIABLE_NAME)))

        return entityManager.createQuery(query).setMaxResults(1).resultList.firstOrNull()
    }

    /**
     * Returns the deadline warning windows of every zaaktype configuration that has at least one of them.
     * Only these three columns are read, so the configurations and their child rows are not loaded.
     */
    fun listDeadlineWarningWindows(): List<ZaaktypeDeadlineWarningWindows> {
        val criteriaBuilder = entityManager.criteriaBuilder
        val query = criteriaBuilder.createQuery(ZaaktypeDeadlineWarningWindows::class.java)
        val root = query.from(ZaaktypeConfiguration::class.java)
        val einddatumGeplandWaarschuwing = root.get<Int>(ZaaktypeConfiguration::einddatumGeplandWaarschuwing.name)
        val uiterlijkeEinddatumAfdoeningWaarschuwing =
            root.get<Int>(ZaaktypeConfiguration::uiterlijkeEinddatumAfdoeningWaarschuwing.name)
        query.select(
            criteriaBuilder.construct(
                ZaaktypeDeadlineWarningWindows::class.java,
                root.get<UUID>(ZAAKTYPE_UUID_VARIABLE_NAME),
                einddatumGeplandWaarschuwing,
                uiterlijkeEinddatumAfdoeningWaarschuwing
            )
        ).where(
            criteriaBuilder.or(
                criteriaBuilder.isNotNull(einddatumGeplandWaarschuwing),
                criteriaBuilder.isNotNull(uiterlijkeEinddatumAfdoeningWaarschuwing)
            )
        )
        return entityManager.createQuery(query).resultList
    }

    fun isSmartDocumentsEnabled(zaaktypeUUID: UUID): Boolean =
        readZaaktypeConfiguration(zaaktypeUUID)?.isSmartDocumentsEnabled ?: false
}
