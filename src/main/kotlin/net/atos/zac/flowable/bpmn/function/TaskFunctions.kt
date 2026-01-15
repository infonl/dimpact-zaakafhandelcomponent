/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.flowable.bpmn.function

import net.atos.zac.flowable.FlowableHelper
import org.flowable.common.engine.impl.context.Context
import org.flowable.engine.impl.util.CommandContextUtil
import org.flowable.task.api.TaskInfo

fun behandelaar(taskDefinitionKey: String): String? =
    taskByDefinitionKey(taskDefinitionKey)?.assignee

fun groep(taskDefinitionKey: String): String? =
    taskByDefinitionKey(taskDefinitionKey)?.identityLinks?.firstOrNull { it.type == "candidate" }?.groupId

private fun taskByDefinitionKey(taskDefinitionKey: String): TaskInfo? =
    FlowableHelper.getInstance().flowableHistoryService
        .createHistoricTaskInstanceQuery()
        .processInstanceId(getProcessInstanceId())
        .taskDefinitionKey(taskDefinitionKey)
        .orderByTaskCreateTime().desc()
        .list().firstOrNull()

private fun getProcessInstanceId(): String =
    Context.getCommandContext()?.let {
        CommandContextUtil.getInvolvedExecutions(it).values.lastOrNull()?.processInstanceId
    } ?: error("No active Flowable BPMN execution found")
