/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.signalering.log

import nl.info.zac.log.LogEvent
import nl.info.zac.signalering.model.SignaleringSubject
import nl.info.zac.signalering.model.SignaleringType

class SignaleringEventHandled(
    val signaleringType: SignaleringType.Type,
    val objectId: String,
    val result: SignaleringEventResult,
    val isDashboard: Boolean,
    val isMail: Boolean
) : LogEvent("Signalering event handled")
