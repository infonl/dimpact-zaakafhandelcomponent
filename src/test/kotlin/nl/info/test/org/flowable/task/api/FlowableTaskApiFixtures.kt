/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.test.org.flowable.task.api

import org.flowable.identitylink.api.IdentityLinkInfo
import org.flowable.task.api.DelegationState
import org.flowable.task.api.Task
import java.util.Date

@Suppress("LongParameterList")
fun createTestTask(
    id: String = "fakeId",
    name: String = "fakeName",
    description: String = "fakeDescription",
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
    state: String = "fakeState",
    inProgressStartTime: Date = Date(),
    inProgressStartedBy: String = "fakeInProgressStartedBy",
    claimTime: Date = Date(),
    claimedBy: String = "fakeClaimedBy",
    suspendedTime: Date = Date(),
    suspendedBy: String = "fakeSuspendedBy",
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
