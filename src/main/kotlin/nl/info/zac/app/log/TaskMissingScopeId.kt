/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.log

import nl.info.zac.log.LogEvent

class TaskMissingScopeId(
    val taakId: String,
    val taskName: String?,
    val createTime: String?
) : LogEvent("Task missing scopeId")
