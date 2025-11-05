/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.admin

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import jakarta.transaction.Transactional.TxType.REQUIRES_NEW
import nl.info.zac.admin.exception.ZaaktypeConfigurationNotFoundException
import nl.info.zac.admin.model.ZaaktypeBpmnConfiguration
import nl.info.zac.admin.model.ZaaktypeBpmnConfiguration.Companion.CREATIEDATUM_VARIABLE_NAME
import nl.info.zac.admin.model.ZaaktypeBpmnConfiguration.Companion.PRODUCTAANVRAAGTYPE_VARIABLE_NAME
import nl.info.zac.admin.model.ZaaktypeBpmnConfiguration.Companion.ZAAKTYPE_OMSCHRIJVING
import nl.info.zac.admin.model.ZaaktypeBpmnConfiguration.Companion.ZAAKTYPE_UUID_VARIABLE_NAME
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.util.UUID
import java.util.logging.Logger
import kotlin.jvm.optionals.getOrNull

@ApplicationScoped
@Transactional
@NoArgConstructor
@AllOpen
class ZaaktypeBpmnConfigurationBeheerService @Inject constructor(
    private val entityManager: EntityManager
) {
    companion object {
        private val LOG = Logger.getLogger(ZaaktypeBpmnConfigurationBeheerService::class.java.name)
    }

    fun storeConfiguration(zaaktypeBpmnConfiguration: ZaaktypeBpmnConfiguration): ZaaktypeBpmnConfiguration {
        zaaktypeBpmnConfiguration.id?.let {
            if (findConfiguration(zaaktypeBpmnConfiguration.zaaktypeUuid) == null) {
                LOG.warning("BPMN configuration with zaaktype UUID '$it' not found, creating new configuration")
                zaaktypeBpmnConfiguration.id = null
            }
        }

        return if (zaaktypeBpmnConfiguration.id != null) {
            entityManager.merge(zaaktypeBpmnConfiguration)
        } else {
            entityManager.persist(zaaktypeBpmnConfiguration)
            entityManager.flush()
            findConfiguration(zaaktypeBpmnConfiguration.zaaktypeUuid)
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
    @Transactional(REQUIRES_NEW)
    fun findConfiguration(zaaktypeUUID: UUID): ZaaktypeBpmnConfiguration? =
        entityManager.criteriaBuilder.let { criteriaBuilder ->
            criteriaBuilder.createQuery(ZaaktypeBpmnConfiguration::class.java).let { query ->
                query.from(ZaaktypeBpmnConfiguration::class.java).let { root ->
                    query.select(root)
                        .where(criteriaBuilder.equal(root.get<UUID>(ZAAKTYPE_UUID_VARIABLE_NAME), zaaktypeUUID))
                        .orderBy(criteriaBuilder.desc(root.get<Any>(CREATIEDATUM_VARIABLE_NAME)))
                    entityManager.createQuery(query).setMaxResults(1).resultStream.findFirst().getOrNull()
                }
            }
        }

    /**
     * Returns the zaaktype - BPMN process definition relation for the given zaaktype UUID or 'null'
     * if no BPMN process definition could be found for the given zaaktype UUID.
     */
    fun findConfiguration(zaaktypeDescription: String): ZaaktypeBpmnConfiguration? =
        entityManager.criteriaBuilder.let { criteriaBuilder ->
            criteriaBuilder.createQuery(ZaaktypeBpmnConfiguration::class.java).let { query ->
                query.from(ZaaktypeBpmnConfiguration::class.java).let { root ->
                    query.select(root)
                        .where(criteriaBuilder.equal(root.get<String>(ZAAKTYPE_OMSCHRIJVING), zaaktypeDescription))
                        .orderBy(criteriaBuilder.desc(root.get<Any>(CREATIEDATUM_VARIABLE_NAME)))
                    entityManager.createQuery(query).setMaxResults(1).resultStream.findFirst().getOrNull()
                }
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

    /**
     * Finds the active [ZaaktypeBpmnConfiguration] for the specified productaanvraag type.
     * If multiple active ZaaktypeBpmnConfigurations are found, this indicates an error in the configuration.
     * There should be at most only one active ZaaktypeBpmnConfiguration for each productaanvraagtype.
     *
     * @return the first found [ZaaktypeBpmnConfiguration] or a null if none are found
     */
    fun findConfigurationByProductAanvraagType(productAanvraagType: String): ZaaktypeBpmnConfiguration? =
        entityManager.criteriaBuilder.let { criteriaBuilder ->
            criteriaBuilder.createQuery(ZaaktypeBpmnConfiguration::class.java).let { query ->
                query.from(ZaaktypeBpmnConfiguration::class.java).let { root ->
                    query.select(root)
                        .where(
                            criteriaBuilder.equal(
                                root.get<String>(PRODUCTAANVRAAGTYPE_VARIABLE_NAME),
                                productAanvraagType
                            )
                        )
                        .orderBy(criteriaBuilder.desc(root.get<Any>(CREATIEDATUM_VARIABLE_NAME)))
                    entityManager.createQuery(query).setMaxResults(1).resultStream.findFirst().getOrNull()
                }
            }
        }
}
