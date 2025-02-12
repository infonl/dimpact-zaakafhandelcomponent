/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.flowable.cmmn

import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Instance
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import net.atos.client.zgw.util.extractUuid
import net.atos.client.zgw.zrc.model.Zaak
import net.atos.client.zgw.ztc.model.generated.ZaakType
import net.atos.zac.admin.model.ZaakafhandelParameters
import net.atos.zac.authentication.LoggedInUser
import net.atos.zac.flowable.ZaakVariabelenService
import net.atos.zac.flowable.cmmn.exception.CaseDefinitionNotFoundException
import net.atos.zac.flowable.cmmn.exception.OpenTaskItemNotFoundException
import net.atos.zac.flowable.task.CreateUserTaskInterceptor
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import org.flowable.cmmn.api.CmmnRepositoryService
import org.flowable.cmmn.api.CmmnRuntimeService
import org.flowable.cmmn.api.repository.CaseDefinition
import org.flowable.cmmn.api.runtime.PlanItemDefinitionType
import org.flowable.cmmn.api.runtime.PlanItemInstance
import org.flowable.cmmn.model.HumanTask
import org.flowable.cmmn.model.UserEventListener
import org.flowable.common.engine.api.FlowableObjectNotFoundException
import java.util.Date
import java.util.UUID
import java.util.logging.Logger

@ApplicationScoped
@Transactional
@AllOpen
@NoArgConstructor
@Suppress("TooManyFunctions")
class CMMNService @Inject constructor(
    private val cmmnRuntimeService: CmmnRuntimeService,
    private val cmmnRepositoryService: CmmnRepositoryService,
    private val loggedInUserInstance: Instance<LoggedInUser>
) {
    companion object {
        private val LOG = Logger.getLogger(CMMNService::class.java.getName())
    }

    fun listHumanTaskPlanItems(zaakUUID: UUID): MutableList<PlanItemInstance> =
        cmmnRuntimeService.createPlanItemInstanceQuery()
            .caseVariableValueEquals(ZaakVariabelenService.VAR_ZAAK_UUID, zaakUUID)
            .planItemInstanceStateEnabled()
            .planItemDefinitionType(PlanItemDefinitionType.HUMAN_TASK)
            .list()

    fun listProcessTaskPlanItems(zaakUUID: UUID): MutableList<PlanItemInstance> =
        cmmnRuntimeService.createPlanItemInstanceQuery()
            .caseVariableValueEquals(ZaakVariabelenService.VAR_ZAAK_UUID, zaakUUID)
            .planItemInstanceStateEnabled()
            .planItemDefinitionType(PlanItemDefinitionType.PROCESS_TASK)
            .list()

    fun listUserEventListenerPlanItems(zaakUUID: UUID): List<PlanItemInstance> =
        cmmnRuntimeService.createPlanItemInstanceQuery()
            .caseVariableValueEquals(ZaakVariabelenService.VAR_ZAAK_UUID, zaakUUID)
            .planItemInstanceStateAvailable()
            .planItemDefinitionType(PlanItemDefinitionType.USER_EVENT_LISTENER)
            .list()

    fun startCase(
        zaak: Zaak,
        zaaktype: ZaakType,
        zaakafhandelParameters: ZaakafhandelParameters,
        zaakData: Map<String, Any>?
    ) {
        val caseDefinitionKey = zaakafhandelParameters.getCaseDefinitionID()
        LOG.info("Starting zaak '${zaak.uuid}' using CMMN model '$caseDefinitionKey'")
        try {
            val caseInstanceBuilder = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey(caseDefinitionKey)
                .businessKey(zaak.uuid.toString())
                .variable(ZaakVariabelenService.VAR_ZAAK_UUID, zaak.uuid)
                .variable(ZaakVariabelenService.VAR_ZAAK_IDENTIFICATIE, zaak.identificatie)
                .variable(ZaakVariabelenService.VAR_ZAAKTYPE_UUUID, zaaktype.url.extractUuid())
                .variable(ZaakVariabelenService.VAR_ZAAKTYPE_OMSCHRIJVING, zaaktype.omschrijving)
            zaakData?.let(caseInstanceBuilder::variables)
            caseInstanceBuilder.start()
        } catch (_: FlowableObjectNotFoundException) {
            LOG.severe(
                "CMMN model '$caseDefinitionKey' for zaak '${zaak.uuid}' could not be found. Zaak is not started.",
            )
        }
    }

    /**
     * Terminate the case for a zaak.
     * This also terminates all open tasks related to the case,
     * This will also call {@Link EndCaseLifecycleListener}
     *
     * @param zaakUUID UUID of the zaak for which the case should be terminated.
     */
    fun terminateCase(zaakUUID: UUID) =
        cmmnRuntimeService.createCaseInstanceQuery()
            .variableValueEquals(ZaakVariabelenService.VAR_ZAAK_UUID, zaakUUID)
            .singleResult()?.let {
                cmmnRuntimeService.terminateCaseInstance(it.id)
            }

    @Suppress("LongParameterList")
    fun startHumanTaskPlanItem(
        planItemInstanceId: String?,
        groupId: String,
        assignee: String?,
        dueDate: Date?,
        description: String?,
        taakdata: Map<String, String>?,
        zaakUUID: UUID
    ) {
        cmmnRuntimeService.createPlanItemInstanceTransitionBuilder(planItemInstanceId)
            .transientVariable(
                ZacCreateHumanTaskInterceptor.Companion.VAR_TRANSIENT_OWNER,
                loggedInUserInstance.get().id
            )
            .transientVariable(ZacCreateHumanTaskInterceptor.Companion.VAR_TRANSIENT_CANDIDATE_GROUP, groupId)
            .transientVariable(ZacCreateHumanTaskInterceptor.Companion.VAR_TRANSIENT_ASSIGNEE, assignee)
            .transientVariable(ZacCreateHumanTaskInterceptor.Companion.VAR_TRANSIENT_ZAAK_UUID, zaakUUID)
            .transientVariable(ZacCreateHumanTaskInterceptor.Companion.VAR_TRANSIENT_DUE_DATE, dueDate)
            .transientVariable(ZacCreateHumanTaskInterceptor.Companion.VAR_TRANSIENT_DESCRIPTION, description)
            .transientVariable(ZacCreateHumanTaskInterceptor.Companion.VAR_TRANSIENT_TAAKDATA, taakdata)
            .start()
    }

    fun startUserEventListenerPlanItem(planItemInstanceId: String) =
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstanceId)

    fun startProcessTaskPlanItem(planItemInstanceId: String, processData: Map<String, Any>) =
        cmmnRuntimeService.createPlanItemInstanceTransitionBuilder(planItemInstanceId)
            .childTaskVariables(
                cmmnRuntimeService.getVariables(readOpenPlanItem(planItemInstanceId).caseInstanceId)
            )
            .childTaskVariables(processData)
            .childTaskVariable(CreateUserTaskInterceptor.VAR_PROCESS_OWNER, loggedInUserInstance.get().id)
            .start()

    fun readOpenPlanItem(planItemInstanceId: String): PlanItemInstance {
        return cmmnRuntimeService.createPlanItemInstanceQuery()
            .planItemInstanceId(planItemInstanceId)
            .singleResult() ?: throw OpenTaskItemNotFoundException(
            "No open plan item found with plan item instance id '$planItemInstanceId'"
        )
    }

    fun listCaseDefinitions(): MutableList<CaseDefinition> =
        cmmnRepositoryService.createCaseDefinitionQuery().latestVersion().list()

    fun readCaseDefinition(caseDefinitionKey: String): CaseDefinition {
        return cmmnRepositoryService.createCaseDefinitionQuery()
            .caseDefinitionKey(caseDefinitionKey)
            .latestVersion()
            .singleResult() ?: throw CaseDefinitionNotFoundException(
            "No case definition found for case definition key: '%$caseDefinitionKey'"
        )
    }

    fun listUserEventListeners(caseDefinitionKey: String): MutableList<UserEventListener> =
        cmmnRepositoryService.getCmmnModel(caseDefinitionKey)
            .primaryCase
            .findPlanItemDefinitionsOfType<UserEventListener>(UserEventListener::class.java)

    fun listHumanTasks(caseDefinitionKey: String): MutableList<HumanTask> =
        cmmnRepositoryService.getCmmnModel(caseDefinitionKey)
            .primaryCase
            .findPlanItemDefinitionsOfType<HumanTask?>(HumanTask::class.java)
}
