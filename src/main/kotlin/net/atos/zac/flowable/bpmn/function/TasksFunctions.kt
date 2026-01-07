/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.flowable.bpmn.function

import net.atos.zac.flowable.FlowableHelper

/**
 * Needs to be package level as [Method:invoke] checks for `Modifier.isStatic(modifiers)`
 * @JVMStatic does *NOT* generate static modifier @ 2026-01-07, but only `public final`
 */
fun behandelaar(taskId: String): String? =
    FlowableHelper.getInstance().flowableHistoryService
        .createHistoricTaskInstanceQuery()
        .taskDefinitionKey(taskId)
        .orderByHistoricTaskInstanceEndTime().desc()
        .list().firstOrNull()?.assignee
