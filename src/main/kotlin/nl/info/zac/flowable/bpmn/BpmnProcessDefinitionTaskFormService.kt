/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.flowable.bpmn

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.json.Json
import jakarta.json.JsonObject
import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import nl.info.zac.flowable.bpmn.model.BpmnProcessDefinitionId
import nl.info.zac.flowable.bpmn.model.BpmnProcessDefinitionTaskForm
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import org.apache.commons.lang3.StringUtils

@ApplicationScoped
@Transactional(Transactional.TxType.SUPPORTS)
@NoArgConstructor
@AllOpen
class BpmnProcessDefinitionTaskFormService @Inject constructor(
    private val entityManager: EntityManager
) {
    fun readForm(bpmnProcessDefinitionId: BpmnProcessDefinitionId, name: String): JsonObject {
        return findForm(bpmnProcessDefinitionId, name)?.content?.toJsonObject()
            ?: throw NoSuchElementException("No BpmnProcessDefinitionTaskForm found with name: '$name'")
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

    @Transactional(Transactional.TxType.REQUIRED)
    fun addForm(bpmnProcessDefinitionId: BpmnProcessDefinitionId, filename: String, content: String) {
        val form = content.toJsonObject()
        BpmnProcessDefinitionTaskForm().apply {
            this.bpmnProcessDefinition = bpmnProcessDefinitionId.name
            this.bpmnProcessDefinitionVersion = bpmnProcessDefinitionId.version
            this.filename = filename
            this.content = content
            name = form.getJsonString("name")?.string ?: filename.removeSuffix(".json")
            title = form.getJsonString("title")?.string ?: StringUtils.EMPTY
            findForm(bpmnProcessDefinitionId, name)?.let {
                id = it.id
            }
        }.let { entityManager.merge(it) }
    }

    @Transactional(Transactional.TxType.REQUIRED)
    fun deleteForm(id: Long) {
        entityManager.find(BpmnProcessDefinitionTaskForm::class.java, id)?.let { entityManager.remove(it) }
    }

    private fun findForm(
        bpmnProcessDefinitionId: BpmnProcessDefinitionId,
        name: String
    ): BpmnProcessDefinitionTaskForm? =
        entityManager.criteriaBuilder.let { criteriaBuilder ->
            criteriaBuilder.createQuery(BpmnProcessDefinitionTaskForm::class.java).let { query ->
                query.from(BpmnProcessDefinitionTaskForm::class.java).let {
                    query.where(
                        criteriaBuilder.equal(
                            it.get<String>("bpmnProcessDefinition"),
                            bpmnProcessDefinitionId.name
                        ),
                        criteriaBuilder.equal(
                            it.get<String>("bpmnProcessDefinitionVersion"),
                            bpmnProcessDefinitionId.version
                        ),
                        criteriaBuilder.equal(it.get<String>("name"), name)
                    )
                }
                entityManager.createQuery(query).resultStream.findFirst().orElse(null)
            }
        }

    private fun String.toJsonObject(): JsonObject = Json.createReader(this.reader()).readObject()
}
