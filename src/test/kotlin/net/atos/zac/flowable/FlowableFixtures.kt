/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.flowable

import org.flowable.cmmn.api.runtime.PlanItemInstance
import org.flowable.identitylink.api.IdentityLinkInfo
import org.flowable.task.api.DelegationState
import org.flowable.task.api.Task
import java.util.Date

@Suppress("LongParameterList")
fun createTestPlanItemInstance(
    id: String = "dummyId",
    name: String = "dummyName",
    state: String = "dummyState",
    caseDefinitionId: String = "dummyCaseDefinitionId",
    derivedCaseDefinitionId: String = "dummyDerivedCaseDefinitionId",
    caseInstanceId: String = "dummyCaseInstanceId",
    stageInstanceId: String = "dummyStageInstanceId",
    isStage: Boolean = false,
    elementId: String = "dummyElementId",
    planItemDefinitionId: String = "dummyPlanItemDefinitionId",
    planItemDefinitionType: String = "dummyPlanItemDefinitionType",
) = TestPlanItemInstance(
    id,
    name,
    state,
    caseDefinitionId,
    derivedCaseDefinitionId,
    caseInstanceId,
    stageInstanceId,
    isStage,
    elementId,
    planItemDefinitionId,
    planItemDefinitionType
)

@Suppress("LongParameterList")
fun createTestTask(
    id: String = "dummyId",
    name: String = "dummyName",
    description: String = "dummyDescription",
    priority: Int = 0,
    owner: String? = null,
    assignee: String? = null,
    delegationState: DelegationState = DelegationState.PENDING,
    createTime: Date = Date(),
    dueDate: Date? = null,
    category: String? = null,
    parentTaskId: String? = null,
    tenantId: String? = null,
    formKey: String? = null,
    taskDefinitionKey: String? = null,
    processInstanceId: String? = null,
    executionId: String? = null,
    processDefinitionId: String? = null,
    scopeId: String? = null,
    subScopeId: String? = null,
    scopeType: String = "cmmn",
    scopeDefinitionId: String? = null,
    taskDefinitionId: String? = null,
    suspended: Boolean = false,
    taskLocalVariables: Map<String, Any> = emptyMap(),
    processVariables: Map<String, Any> = emptyMap(),
    identityLinks: List<IdentityLinkInfo> = emptyList(),
    state: String = "dummyState",
    inProgressStartTime: Date = Date(),
    inProgressStartedBy: String = "dummyInProgressStartedBy",
    claimTime: Date = Date(),
    claimedBy: String = "dummyClaimedBy",
    suspendedTime: Date = Date(),
    suspendedBy: String = "dummySuspendedBy",
    inProgressStartDueDate: Date = Date(),
    caseVariables: Map<String, Any>? = null
) = TestTask(
    id,
    name,
    description,
    priority,
    owner,
    assignee,
    delegationState,
    createTime,
    dueDate,
    category,
    parentTaskId,
    tenantId,
    formKey,
    taskDefinitionKey,
    processInstanceId,
    executionId,
    processDefinitionId,
    scopeId,
    subScopeId,
    scopeType,
    scopeDefinitionId,
    taskDefinitionId,
    suspended,
    taskLocalVariables,
    processVariables,
    identityLinks,
    state,
    inProgressStartTime,
    inProgressStartedBy,
    claimTime,
    claimedBy,
    suspendedTime,
    suspendedBy,
    inProgressStartDueDate,
    caseVariables
)

data class TestTask(
    private val id: String,
    private var name: String,
    private var description: String,
    private var priority: Int,
    private var owner: String?,
    private var assignee: String?,
    private var delegationState: DelegationState,
    private val createTime: Date,
    private var dueDate: Date?,
    private var category: String?,
    private var parentTaskId: String?,
    private var tenantId: String?,
    private var formKey: String?,
    private val taskDefinitionKey: String?,
    private val processInstanceId: String?,
    private val executionId: String?,
    private val processDefinitionId: String?,
    private val scopeId: String?,
    private val subScopeId: String?,
    private val scopeType: String?,
    private val scopeDefinitionId: String?,
    private val taskDefinitionId: String?,
    val suspended: Boolean,
    private val taskLocalVariables: Map<String, Any>,
    private val processVariables: Map<String, Any>,
    private val identityLinks: List<IdentityLinkInfo>,
    private val state: String,
    private val inProgressStartTime: Date,
    private val inProgressStartedBy: String,
    private val claimTime: Date,
    private val claimedBy: String,
    private val suspendedTime: Date,
    private val suspendedBy: String,
    private val inProgressStartDueDate: Date,
    private val caseVariables: Map<String, Any>? = null
) : Task {
    override fun getId() = id
    override fun getName() = name
    override fun getDescription() = description
    override fun getPriority() = priority
    override fun getOwner() = owner
    override fun getAssignee() = assignee
    override fun getProcessInstanceId() = processInstanceId
    override fun getExecutionId() = executionId
    override fun getTaskDefinitionId() = taskDefinitionId
    override fun getProcessDefinitionId() = processDefinitionId
    override fun getScopeId() = scopeId
    override fun getSubScopeId() = subScopeId
    override fun getScopeType() = scopeType
    override fun getScopeDefinitionId() = scopeDefinitionId
    override fun getPropagatedStageInstanceId() = processInstanceId
    override fun getState() = state
    override fun getCreateTime() = createTime
    override fun getInProgressStartTime() = inProgressStartTime
    override fun getInProgressStartedBy() = inProgressStartedBy
    override fun getClaimTime() = claimTime
    override fun getClaimedBy() = claimedBy
    override fun getSuspendedTime() = suspendedTime
    override fun getSuspendedBy() = suspendedBy
    override fun getTaskDefinitionKey() = taskDefinitionKey
    override fun getInProgressStartDueDate() = inProgressStartDueDate
    override fun getDueDate() = dueDate
    override fun getCategory() = category
    override fun getParentTaskId() = parentTaskId
    override fun getTenantId() = tenantId
    override fun getFormKey() = formKey
    override fun getTaskLocalVariables() = taskLocalVariables
    override fun getProcessVariables() = processVariables
    override fun getCaseVariables() = caseVariables
    override fun getIdentityLinks() = identityLinks

    override fun setName(name: String) { this.name = name }
    override fun setLocalizedName(name: String) { this.name = name }
    override fun setDescription(description: String) { this.description = description }
    override fun setLocalizedDescription(description: String) { this.description = description }
    override fun setPriority(priority: Int) { this.priority = priority }
    override fun setOwner(owner: String) { this.owner = owner }
    override fun setAssignee(assignee: String) { this.assignee = assignee }
    override fun getDelegationState() = delegationState
    override fun setDelegationState(delegationState: DelegationState) { this.delegationState = delegationState }
    override fun setDueDate(dueDate: Date) { this.dueDate = dueDate }
    override fun setCategory(category: String) { this.category = category }
    override fun setParentTaskId(parentTaskId: String) { this.parentTaskId = parentTaskId }
    override fun setTenantId(tenantId: String) { this.tenantId = tenantId }
    override fun setFormKey(formKey: String) { this.formKey = formKey }
    override fun isSuspended() = suspended
}

data class TestPlanItemInstance(
    private val id: String,
    private val name: String,
    private val state: String,
    private val caseDefinitionId: String,
    private val derivedCaseDefinitionId: String,
    private val caseInstanceId: String,
    private val stageInstanceId: String,
    private val isStage: Boolean,
    private val elementId: String,
    private val planItemDefinitionId: String,
    private val planItemDefinitionType: String,
    private val createTime: Date? = null,
    private val lastAvailableTime: Date? = null,
    private val lastUnavailableTime: Date? = null,
    private val lastEnabledTime: Date? = null,
    private val lastDisabledTime: Date? = null,
    private val lastStartedTime: Date? = null,
    private val lastSuspendedTime: Date? = null,
    private val completedTime: Date? = null,
    private val occurredTime: Date? = null,
    private val terminatedTime: Date? = null,
    private val exitTime: Date? = null,
    private val endedTime: Date? = null,
    private val startUserId: String? = null,
    private val referenceId: String? = null,
    private val referenceType: String? = null,
    private val completable: Boolean = false,
    private val entryCriterionId: String? = null,
    private val exitCriterionId: String? = null,
    private val formKey: String? = null,
    private val extraValue: String? = null,
    private val tenantId: String? = null,
    private val planItemInstanceLocalVariables: Map<String, Any>? = null,
    private var localizedName: String? = null
) : PlanItemInstance {
    override fun getId() = id
    override fun getName() = name
    override fun getState() = state
    override fun getCaseDefinitionId() = caseDefinitionId
    override fun getDerivedCaseDefinitionId() = derivedCaseDefinitionId
    override fun getCaseInstanceId() = caseInstanceId
    override fun getStageInstanceId() = stageInstanceId
    override fun isStage() = isStage
    override fun getElementId() = elementId
    override fun getPlanItemDefinitionId() = planItemDefinitionId
    override fun getPlanItemDefinitionType() = planItemDefinitionType
    override fun getCreateTime() = createTime
    override fun getLastAvailableTime() = lastAvailableTime
    override fun getLastUnavailableTime() = lastUnavailableTime
    override fun getLastEnabledTime() = lastEnabledTime
    override fun getLastDisabledTime() = lastDisabledTime
    override fun getLastStartedTime() = lastStartedTime
    override fun getLastSuspendedTime() = lastSuspendedTime
    override fun getCompletedTime() = completedTime
    override fun getOccurredTime() = occurredTime
    override fun getTerminatedTime() = terminatedTime
    override fun getExitTime() = exitTime
    override fun getEndedTime() = endedTime
    override fun getStartUserId() = startUserId
    override fun getReferenceId() = referenceId
    override fun getReferenceType() = referenceType
    override fun isCompletable() = completable
    override fun getEntryCriterionId() = entryCriterionId
    override fun getExitCriterionId() = exitCriterionId
    override fun getFormKey() = formKey
    override fun getExtraValue() = extraValue
    override fun getTenantId() = tenantId
    override fun getPlanItemInstanceLocalVariables() = planItemInstanceLocalVariables
    override fun setLocalizedName(localizedName: String) {
         this.localizedName = localizedName
    }
}

