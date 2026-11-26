/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.signalering.log

import nl.info.zac.log.LogEvent
import nl.info.zac.signalering.model.SignaleringSubject
import nl.info.zac.signalering.model.SignaleringType

class InvalidSignaleringFound(
    val signaleringId: Long?,
    val signaleringType: SignaleringType.Type,
    val subjecttype: SignaleringSubject,
    val subject: String,
    val reason: String
) : LogEvent("Invalid signalering found; deleting it")
