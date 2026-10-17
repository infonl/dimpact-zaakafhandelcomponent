/*
 * SPDX-FileCopyrightText: 2021 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.admin.converter

import jakarta.inject.Inject
import net.atos.zac.app.admin.model.RESTPlanItemDefinition
import net.atos.zac.flowable.cmmn.CmmnService
import nl.info.zac.admin.model.ProcessEngine
import nl.info.zac.admin.model.ZaaktypeProcessBinding
import nl.info.zac.app.admin.model.RestProcessDefinition
import nl.info.zac.app.planitems.model.PlanItemType.HUMAN_TASK
import nl.info.zac.app.planitems.model.PlanItemType.USER_EVENT_LISTENER
import nl.info.zac.flowable.bpmn.BpmnService
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import org.flowable.cmmn.api.repository.CaseDefinition
import org.flowable.cmmn.model.HumanTask
import org.flowable.cmmn.model.UserEventListener
import org.flowable.engine.repository.ProcessDefinition

@AllOpen
@NoArgConstructor
class RestProcessDefinitionConverter @Inject constructor(
    private val cmmnService: CmmnService,
    private val bpmnService: BpmnService
) {
    fun listProcessDefinitions(): List<RestProcessDefinition> =
        cmmnService.listCaseDefinitions().map { it.toRestProcessDefinition(inclusiefRelaties = true) } +
            bpmnService.listProcessDefinitions().map { it.toRestProcessDefinition() }

    fun toRestProcessDefinition(processBinding: ZaaktypeProcessBinding, inclusiefRelaties: Boolean) =
        when (processBinding.processEngine) {
            ProcessEngine.CMMN -> cmmnService.readCaseDefinition(processBinding.definitionKey)
                .toRestProcessDefinition(inclusiefRelaties)
            ProcessEngine.BPMN -> RestProcessDefinition(
                processEngine = ProcessEngine.BPMN,
                key = processBinding.definitionKey,
                naam = bpmnService.findProcessDefinitionByProcessDefinitionKey(processBinding.definitionKey)?.name
            )
        }

    private fun CaseDefinition.toRestProcessDefinition(inclusiefRelaties: Boolean) =
        RestProcessDefinition(processEngine = ProcessEngine.CMMN, key = key, naam = name).apply {
            if (inclusiefRelaties) {
                humanTaskDefinitions = cmmnService.listHumanTasks(id).map { it.toRestPlanItemDefinition() }
                userEventListenerDefinitions = cmmnService.listUserEventListeners(id)
                    .map { it.toRestPlanItemDefinition() }
            }
        }

    private fun ProcessDefinition.toRestProcessDefinition() =
        RestProcessDefinition(processEngine = ProcessEngine.BPMN, key = key, naam = name)

    private fun HumanTask.toRestPlanItemDefinition() = RESTPlanItemDefinition(id, name, HUMAN_TASK)

    private fun UserEventListener.toRestPlanItemDefinition() = RESTPlanItemDefinition(id, name, USER_EVENT_LISTENER)
}
