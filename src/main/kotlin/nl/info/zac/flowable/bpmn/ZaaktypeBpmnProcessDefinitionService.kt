/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.flowable.bpmn

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import nl.info.zac.flowable.bpmn.model.ZaaktypeBpmnProcessDefinition
import nl.info.zac.flowable.bpmn.model.ZaaktypeBpmnProcessDefinition.Companion.PRODUCTAANVRAAGTTYPE_VARIABELE_NAME
import nl.info.zac.flowable.bpmn.model.ZaaktypeBpmnProcessDefinition.Companion.ZAAKTYPE_UUID_VARIABLE_NAME
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.util.UUID

@ApplicationScoped
@Transactional
@NoArgConstructor
@AllOpen
class ZaaktypeBpmnProcessDefinitionService @Inject constructor(
    private val entityManager: EntityManager
) {
    fun createZaaktypeBpmnProcessDefinition(zaaktypeBpmnProcessDefinition: ZaaktypeBpmnProcessDefinition) {
        entityManager.persist(zaaktypeBpmnProcessDefinition)
    }

    fun deleteZaaktypeBpmnProcessDefinition(zaaktypeBpmnProcessDefinition: ZaaktypeBpmnProcessDefinition) {
        entityManager.remove(zaaktypeBpmnProcessDefinition)
    }

    /**
     * Returns the zaaktype - BPMN process definition relation for the given zaaktype UUID or 'null'
     * if no BPMN process definition could be found for the given zaaktype UUID.
     */
    fun findZaaktypeProcessDefinitionByZaaktypeUuid(zaaktypeUUID: UUID): ZaaktypeBpmnProcessDefinition? =
        entityManager.criteriaBuilder.let { criteriaBuilder ->
            criteriaBuilder.createQuery(ZaaktypeBpmnProcessDefinition::class.java).let { query ->
                query.from(ZaaktypeBpmnProcessDefinition::class.java).let {
                    query.where(criteriaBuilder.equal(it.get<UUID>(ZAAKTYPE_UUID_VARIABLE_NAME), zaaktypeUUID))
                }
                entityManager.createQuery(query).resultStream.findFirst().orElse(null)
            }
        }

    /**
     * Returns a list of all BPMN process definitions.
     */
    fun listBpmnProcessDefinitions(): List<ZaaktypeBpmnProcessDefinition> =
        entityManager.criteriaBuilder.let { criteriaBuilder ->
            criteriaBuilder.createQuery(ZaaktypeBpmnProcessDefinition::class.java).let { query ->
                query.from(ZaaktypeBpmnProcessDefinition::class.java)
                entityManager.createQuery(query).resultList
            }
        }

    fun findByProductAanvraagType(productAanvraagType: String): ZaaktypeBpmnProcessDefinition? =
        entityManager.criteriaBuilder.let { criteriaBuilder ->
            criteriaBuilder.createQuery(ZaaktypeBpmnProcessDefinition::class.java).let { query ->
                query.from(ZaaktypeBpmnProcessDefinition::class.java).let {
                    query.where(
                        criteriaBuilder.equal(it.get<String>(PRODUCTAANVRAAGTTYPE_VARIABELE_NAME), productAanvraagType)
                    )
                }
                entityManager.createQuery(query).resultStream.findFirst().orElse(null)
            }
        }
}
