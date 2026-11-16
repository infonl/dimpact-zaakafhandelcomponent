/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.admin.converter

import jakarta.enterprise.context.RequestScoped
import jakarta.inject.Inject
import nl.info.zac.admin.model.ProcessEngine
import nl.info.zac.admin.model.ZaaktypeProcessBinding
import nl.info.zac.app.admin.model.RestPlanItemDefinition
import nl.info.zac.app.admin.model.RestProcessDefinition
import nl.info.zac.app.planitems.model.PlanItemType.HUMAN_TASK
import nl.info.zac.app.planitems.model.PlanItemType.USER_EVENT_LISTENER
import nl.info.zac.flowable.bpmn.BpmnService
import nl.info.zac.flowable.bpmn.nameOrKey
import nl.info.zac.flowable.cmmn.CmmnService
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import org.flowable.cmmn.api.repository.CaseDefinition
import org.flowable.cmmn.model.HumanTask
import org.flowable.cmmn.model.UserEventListener
import org.flowable.engine.repository.ProcessDefinition

/**
 * Many zaaktype configurations share a few process definitions, while a request can convert hundreds of them.
 * Request scope reads each definition from Flowable once per request, and still lets a request see a definition
 * that was deployed after the previous request.
 */
@RequestScoped
@AllOpen
@NoArgConstructor
class RestProcessDefinitionConverter @Inject constructor(
    private val cmmnService: CmmnService,
    private val bpmnService: BpmnService
) {
    private val caseDefinitionsByKey = mutableMapOf<String, CaseDefinition>()
    private val humanTasksByCaseDefinitionId = mutableMapOf<String, List<HumanTask>>()
    private val userEventListenersByCaseDefinitionId = mutableMapOf<String, List<UserEventListener>>()
    private val bpmnProcessDefinitionNamesByKey = mutableMapOf<String, String?>()

    fun listProcessDefinitions(): List<RestProcessDefinition> =
        cmmnService.listCaseDefinitions().map { it.toRestProcessDefinition(inclusiefRelaties = true) } +
            bpmnService.listProcessDefinitions().filterNot { it.isSuspended }.map { it.toRestProcessDefinition() }

    fun toRestProcessDefinition(processBinding: ZaaktypeProcessBinding, inclusiefRelaties: Boolean) =
        when (processBinding.processEngine) {
            ProcessEngine.CMMN -> caseDefinitionsByKey.getOrPut(processBinding.definitionKey) {
                cmmnService.readCaseDefinition(processBinding.definitionKey)
            }.toRestProcessDefinition(inclusiefRelaties)
            ProcessEngine.BPMN -> RestProcessDefinition(
                processEngine = ProcessEngine.BPMN,
                key = processBinding.definitionKey,
                naam = readBpmnProcessDefinitionName(processBinding.definitionKey)
            )
        }

    private fun readBpmnProcessDefinitionName(processDefinitionKey: String) =
        if (processDefinitionKey in bpmnProcessDefinitionNamesByKey) {
            bpmnProcessDefinitionNamesByKey[processDefinitionKey]
        } else {
            bpmnService.findProcessDefinitionByProcessDefinitionKey(processDefinitionKey)
                ?.nameOrKey()
                .also { bpmnProcessDefinitionNamesByKey[processDefinitionKey] = it }
        }

    private fun CaseDefinition.toRestProcessDefinition(inclusiefRelaties: Boolean) =
        RestProcessDefinition(processEngine = ProcessEngine.CMMN, key = key, naam = name).apply {
            if (inclusiefRelaties) {
                humanTaskDefinitions = humanTasksByCaseDefinitionId.getOrPut(id) { cmmnService.listHumanTasks(id) }
                    .map { it.toRestPlanItemDefinition() }
                userEventListenerDefinitions = userEventListenersByCaseDefinitionId
                    .getOrPut(id) { cmmnService.listUserEventListeners(id) }
                    .map { it.toRestPlanItemDefinition() }
            }
        }

    private fun ProcessDefinition.toRestProcessDefinition() =
        RestProcessDefinition(processEngine = ProcessEngine.BPMN, key = key, naam = nameOrKey())

    private fun HumanTask.toRestPlanItemDefinition() = RestPlanItemDefinition(id, name, HUMAN_TASK)

    private fun UserEventListener.toRestPlanItemDefinition() = RestPlanItemDefinition(id, name, USER_EVENT_LISTENER)
}
