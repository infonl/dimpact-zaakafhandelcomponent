/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.flowable

import org.flowable.identitylink.api.IdentityLinkInfo
import org.flowable.task.api.DelegationState
import java.util.Date

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
