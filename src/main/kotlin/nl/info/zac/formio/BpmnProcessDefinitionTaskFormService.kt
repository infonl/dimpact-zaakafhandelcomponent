/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.formio

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.json.Json
import jakarta.json.JsonObject
import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import nl.info.zac.formio.model.BpmnProcessDefinitionTaskForm
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor

data class TaskFormIdentifier(
    val bpmnProcessDefinition: String,
    val bpmnProcessDefinitionVersion: String,
    val name: String
)

@ApplicationScoped
@Transactional(Transactional.TxType.SUPPORTS)
@NoArgConstructor
@AllOpen
class BpmnProcessDefinitionTaskFormService @Inject constructor(
    private val entityManager: EntityManager
) {
    fun readForm(taskFormIdentifier: TaskFormIdentifier): JsonObject {
        return findForm(taskFormIdentifier)?.content?.toJsonObject()
            ?: throw NoSuchElementException("No BpmnProcessDefinitionTaskForm found with name: '$taskFormIdentifier.name'")
    }

    fun listForms(): List<BpmnProcessDefinitionTaskForm> =
        entityManager.criteriaBuilder.let { criteriaBuilder ->
            criteriaBuilder.createQuery(BpmnProcessDefinitionTaskForm::class.java).let { query ->
                query.from(BpmnProcessDefinitionTaskForm::class.java).let {
                    query.orderBy(
                        criteriaBuilder.asc(it.get<String>("bpmnProcessDefinition")),
                        criteriaBuilder.asc(it.get<String>("bpmnProcessDefinitionVersion")),
                        criteriaBuilder.asc(it.get<String>("name"))
                    )
                }
                entityManager.createQuery(query).resultList
            }
        }

    private fun findForm(taskFormIdentifier: TaskFormIdentifier): BpmnProcessDefinitionTaskForm? =
        entityManager.criteriaBuilder.let { criteriaBuilder ->
            criteriaBuilder.createQuery(BpmnProcessDefinitionTaskForm::class.java).let { query ->
                query.from(BpmnProcessDefinitionTaskForm::class.java).let {
                    query.where(
                        criteriaBuilder.equal(
                            it.get<String>("bpmnProcessDefinition"),
                            taskFormIdentifier.bpmnProcessDefinition
                        ),
                        criteriaBuilder.equal(
                            it.get<String>("bpmnProcessDefinitionVersion"),
                            taskFormIdentifier.bpmnProcessDefinitionVersion
                        ),
                        criteriaBuilder.equal(it.get<String>("name"), taskFormIdentifier.name)
                    )
                }
                entityManager.createQuery(query).resultStream.findFirst().orElse(null)
            }
        }

    private fun String.toJsonObject(): JsonObject = Json.createReader(this.reader()).readObject()
}
