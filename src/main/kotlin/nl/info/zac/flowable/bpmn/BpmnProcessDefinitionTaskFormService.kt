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
import nl.info.zac.flowable.bpmn.exception.ProcessDefinitionNotFoundException
import nl.info.zac.flowable.bpmn.model.BpmnProcessDefinitionTaskForm
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import org.apache.commons.lang3.StringUtils
import org.flowable.engine.RepositoryService
import org.flowable.engine.repository.ProcessDefinition

@ApplicationScoped
@Transactional(Transactional.TxType.SUPPORTS)
@NoArgConstructor
@AllOpen
class BpmnProcessDefinitionTaskFormService @Inject constructor(
    private val entityManager: EntityManager,
    private val repositoryService: RepositoryService
) {
    fun readForm(processDefinitionId: String, name: String): JsonObject {
        readProcessDefinitionByProcessDefinitionId(processDefinitionId).let {
            return findForm(it.key, it.version, name)?.content?.toJsonObject()
                ?: throw NoSuchElementException("No BpmnProcessDefinitionTaskForm found with name: '$name'")
        }
    }

    fun listForms(): List<BpmnProcessDefinitionTaskForm> =
        entityManager.criteriaBuilder.let { criteriaBuilder ->
            criteriaBuilder.createQuery(BpmnProcessDefinitionTaskForm::class.java).let { query ->
                query.from(BpmnProcessDefinitionTaskForm::class.java).let {
                    query.orderBy(
                        criteriaBuilder.asc(
                            it.get<String>(BpmnProcessDefinitionTaskForm::bpmnProcessDefinitionKey.name)
                        ),
                        criteriaBuilder.asc(
                            it.get<Int>(BpmnProcessDefinitionTaskForm::bpmnProcessDefinitionVersion.name)
                        ),
                        criteriaBuilder.asc(it.get<String>(BpmnProcessDefinitionTaskForm::name.name))
                    )
                }
                entityManager.createQuery(query).resultList
            }
        }

    @Transactional(Transactional.TxType.REQUIRED)
    fun addForm(processDefinitionKey: String, filename: String, content: String) {
        readProcessDefinitionByProcessDefinitionKey(processDefinitionKey).let {
            val form = content.toJsonObject()
            BpmnProcessDefinitionTaskForm().apply {
                this.bpmnProcessDefinitionKey = it.key
                this.bpmnProcessDefinitionVersion = it.version
                this.filename = filename
                this.content = content
                name = form.getJsonString("name")?.string ?: filename.removeSuffix(".json")
                title = form.getJsonString("title")?.string ?: StringUtils.EMPTY
                findForm(it.key, it.version, name)?.let {
                    id = it.id
                }
            }.let { entityManager.merge(it) }
        }
    }

    @Transactional(Transactional.TxType.REQUIRED)
    fun deleteForm(processDefinitionKey: String, name: String) {
        readProcessDefinitionByProcessDefinitionKey(processDefinitionKey).let { pd ->
            findForm(pd.key, pd.version, name)?.let { entityManager.remove(it) }
        }
    }

    private fun findForm(
        key: String,
        version: Int,
        name: String
    ): BpmnProcessDefinitionTaskForm? =
        entityManager.criteriaBuilder.let { criteriaBuilder ->
            criteriaBuilder.createQuery(BpmnProcessDefinitionTaskForm::class.java).let { query ->
                query.from(BpmnProcessDefinitionTaskForm::class.java).let {
                    query.where(
                        criteriaBuilder.equal(
                            it.get<String>(BpmnProcessDefinitionTaskForm::bpmnProcessDefinitionKey.name),
                            key
                        ),
                        criteriaBuilder.equal(
                            it.get<Int>(BpmnProcessDefinitionTaskForm::bpmnProcessDefinitionVersion.name),
                            version
                        ),
                        criteriaBuilder.equal(it.get<String>("name"), name)
                    )
                }
                entityManager.createQuery(query).resultStream.findFirst().orElse(null)
            }
        }

    private fun String.toJsonObject(): JsonObject = Json.createReader(this.reader()).readObject()

    private fun readProcessDefinitionByProcessDefinitionKey(processDefinitionKey: String): ProcessDefinition {
        val processDefinition = repositoryService.createProcessDefinitionQuery()
            .processDefinitionKey(processDefinitionKey)
            .active()
            .latestVersion()
            .singleResult()
        return processDefinition
            ?: throw ProcessDefinitionNotFoundException("No process definition found for key '$processDefinitionKey'")
    }
    private fun readProcessDefinitionByProcessDefinitionId(processDefinitionId: String): ProcessDefinition {
        val processDefinition = repositoryService.createProcessDefinitionQuery()
            .processDefinitionId(processDefinitionId)
            .singleResult()
        return processDefinition
            ?: throw ProcessDefinitionNotFoundException("No process definition found for id '$processDefinitionId'")
    }
}
