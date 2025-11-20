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
import nl.info.zac.admin.model.ZaaktypeConfiguration.Companion.CREATIEDATUM_VARIABLE_NAME
import nl.info.zac.admin.model.ZaaktypeConfiguration.Companion.ZAAKTYPE_OMSCHRIJVING_VARIABLE_NAME
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
    private val zaaktypeBpmnConfigurationService: ZaaktypeBpmnConfigurationService
) {
    companion object {
        private val LOG = Logger.getLogger(ZaaktypeConfigurationService::class.java.name)
    }

    fun updateZaaktype(zaaktypeUri: URI) {
        ztcClientService.clearZaaktypeCache()
        ztcClientService.readZaaktype(zaaktypeUri).let {
            if (it.concept) {
                LOG.warning { "Zaaktype '${it.omschrijving}' with UUID ${zaaktypeUri.extractUuid()} is still a concept. Ignoring" }
                return
            }
            getLastCreatedConfiguration(it.omschrijving)?.let { zaaktypeConfiguration ->
                when (zaaktypeConfiguration.getConfigurationType()) {
                    CMMN -> zaaktypeCmmnConfigurationBeheerService.upsertZaaktypeCmmnConfiguration(it)
                    BPMN -> zaaktypeBpmnConfigurationService.copyConfiguration(it)
                }
            } ?: LOG.warning {
                "Zaaktype '${it.omschrijving}' with UUID ${zaaktypeUri.extractUuid()} has no known configuration. Ignoring"
            }
        }
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
}
