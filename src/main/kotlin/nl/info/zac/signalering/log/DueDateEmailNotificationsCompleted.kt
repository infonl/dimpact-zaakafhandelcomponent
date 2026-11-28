/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.signalering.log

import nl.info.zac.log.LogEvent
import nl.info.zac.log.Outcome
import nl.info.zac.signalering.model.SignaleringSubject
import nl.info.zac.signalering.model.SignaleringType

class DueDateEmailNotificationsCompleted(
    val subjecttype: SignaleringSubject,
    val streefdatumWarningCount: Int?,
    val fataledatumWarningCount: Int,
    val outcome: Outcome,
    val durationMs: Long,
    val errorType: String?
) : LogEvent("Sending due date email notifications completed")
