/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.flowable.log

import nl.info.zac.log.Employee
import nl.info.zac.log.LogEvent

class UserTaskCompleted(
    val taakId: String,
    val taskName: String?,
    val processInstanceId: String?,
    val processDefinitionId: String?,
    val assignee: Employee?
) : LogEvent("User task completed")
