/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.signalering.log

import nl.info.zac.log.LogEvent
import nl.info.zac.signalering.model.SignaleringSubject
import nl.info.zac.signalering.model.SignaleringType

class DueDateEmailNotificationsSent(
    val subjecttype: SignaleringSubject,
    val streefdatumWarningCount: Int?,
    val fataledatumWarningCount: Int
) : LogEvent("Finished sending due date email notifications")
