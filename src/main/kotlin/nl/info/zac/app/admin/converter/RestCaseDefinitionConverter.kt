/*
 * SPDX-FileCopyrightText: 2021 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.admin.converter

import jakarta.inject.Inject
import net.atos.zac.app.admin.model.RESTCaseDefinition
import net.atos.zac.app.admin.model.RESTPlanItemDefinition
import net.atos.zac.flowable.cmmn.CmmnService
import nl.info.zac.app.planitems.model.PlanItemType.HUMAN_TASK
import nl.info.zac.app.planitems.model.PlanItemType.USER_EVENT_LISTENER
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import org.flowable.cmmn.api.repository.CaseDefinition
import org.flowable.cmmn.model.HumanTask
import org.flowable.cmmn.model.UserEventListener

@AllOpen
@NoArgConstructor
class RestCaseDefinitionConverter @Inject constructor(
    private val cmmnService: CmmnService
) {
    fun convertToRestCaseDefinition(caseDefinitionKey: String, inclusiefRelaties: Boolean): RESTCaseDefinition =
        convertToRestCaseDefinition(cmmnService.readCaseDefinition(caseDefinitionKey), inclusiefRelaties)

    fun convertToRestCaseDefinition(caseDefinition: CaseDefinition, inclusiefRelaties: Boolean): RESTCaseDefinition =
        RESTCaseDefinition(caseDefinition.name, caseDefinition.key).apply {
            if (inclusiefRelaties) {
                humanTaskDefinitions = cmmnService.listHumanTasks(caseDefinition.id).map { it.toRestPlanItemDefinition() }
                userEventListenerDefinitions = cmmnService.listUserEventListeners(caseDefinition.id)
                    .map { it.toRestPlanItemDefinition() }
            }
        }

    private fun HumanTask.toRestPlanItemDefinition() = RESTPlanItemDefinition(id, name, HUMAN_TASK)

    private fun UserEventListener.toRestPlanItemDefinition() = RESTPlanItemDefinition(id, name, USER_EVENT_LISTENER)
}
