/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.signalering.log

import nl.info.zac.log.LogEvent
import nl.info.zac.log.Outcome

class OldSignaleringenDeletionCompleted(
    val olderThanDays: Long,
    val deletedCount: Int?,
    val outcome: Outcome,
    val durationMs: Long,
    val errorType: String?
) : LogEvent("Deletion of signaleringen older than the retention period completed")
