/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.admin

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import nl.info.zac.admin.exception.ZaaktypeConfigurationNotFoundException
import nl.info.zac.flowable.bpmn.model.ZaaktypeBpmnConfiguration
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.util.UUID
import java.util.logging.Logger

@ApplicationScoped
@Transactional
@NoArgConstructor
@AllOpen
class ZaaktypeBpmnConfigurationService @Inject constructor(
    private val entityManager: EntityManager,
) {
    companion object {
        private val LOG = Logger.getLogger(ZaaktypeBpmnConfigurationService::class.java.name)
    }

    fun storeConfiguration(
        zaaktypeBpmnConfiguration: ZaaktypeBpmnConfiguration
    ): ZaaktypeBpmnConfiguration {
        zaaktypeBpmnConfiguration.id?.let {
            if (findConfigurationByZaaktypeUuid(zaaktypeBpmnConfiguration.zaaktypeUuid) == null) {
                LOG.warning("BPMN configuration with zaaktype UUID '$it' not found, creating new configuration")
                zaaktypeBpmnConfiguration.id = null
            }
        }

        return if (zaaktypeBpmnConfiguration.id != null) {
            entityManager.merge(zaaktypeBpmnConfiguration)
        } else {
            entityManager.persist(zaaktypeBpmnConfiguration)
            entityManager.flush()
            findConfigurationByZaaktypeUuid(zaaktypeBpmnConfiguration.zaaktypeUuid)
                ?: throw ZaaktypeConfigurationNotFoundException(
                    "BPMN zaaktype configuration for `${zaaktypeBpmnConfiguration.zaaktypeOmschrijving}` not found"
                )
        }
    }

    fun deleteConfiguration(zaaktypeBpmnConfiguration: ZaaktypeBpmnConfiguration) {
        entityManager.remove(zaaktypeBpmnConfiguration)
    }

    /**
     * Returns the zaaktype - BPMN process definition relation for the given zaaktype UUID or 'null'
     * if no BPMN process definition could be found for the given zaaktype UUID.
     */
    fun findConfigurationByZaaktypeUuid(zaaktypeUUID: UUID): ZaaktypeBpmnConfiguration? =
        entityManager.criteriaBuilder.let { criteriaBuilder ->
            criteriaBuilder.createQuery(ZaaktypeBpmnConfiguration::class.java).let { query ->
                query.from(ZaaktypeBpmnConfiguration::class.java).let {
                    query.where(
                        criteriaBuilder.equal(
                            it.get<UUID>(ZaaktypeBpmnConfiguration.ZAAKTYPE_UUID_VARIABLE_NAME),
                            zaaktypeUUID
                        )
                    )
                }
                entityManager.createQuery(query).resultStream.findFirst().orElse(null)
            }
        }

    /**
     * Returns a list of all BPMN process definitions.
     */
    fun listConfigurations(): List<ZaaktypeBpmnConfiguration> =
        entityManager.criteriaBuilder.let { criteriaBuilder ->
            criteriaBuilder.createQuery(ZaaktypeBpmnConfiguration::class.java).let { query ->
                query.from(ZaaktypeBpmnConfiguration::class.java)
                entityManager.createQuery(query).resultList
            }
        }

    fun findConfigurationByProductAanvraagType(productAanvraagType: String): ZaaktypeBpmnConfiguration? =
        entityManager.criteriaBuilder.let { criteriaBuilder ->
            criteriaBuilder.createQuery(ZaaktypeBpmnConfiguration::class.java).let { query ->
                query.from(ZaaktypeBpmnConfiguration::class.java).let {
                    query.where(
                        criteriaBuilder.equal(
                            it.get<String>(ZaaktypeBpmnConfiguration.PRODUCTAANVRAAGTTYPE_VARIABLE_NAME),
                            productAanvraagType
                        )
                    )
                }
                entityManager.createQuery(query).resultStream.findFirst().orElse(null)
            }
        }
}
