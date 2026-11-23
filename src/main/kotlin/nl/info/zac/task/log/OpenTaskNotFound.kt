/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.task.log

import nl.info.zac.log.LogEvent

class OpenTaskNotFound(
    val taakId: String,
    val batchOperation: String
) : LogEvent("No open task found; skipping it")
