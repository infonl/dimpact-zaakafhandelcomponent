/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.signalering.log

import nl.info.zac.log.LogEvent
import nl.info.zac.signalering.model.SignaleringSubject
import nl.info.zac.signalering.model.SignaleringType
import java.time.ZonedDateTime

class SignaleringTimestampReplaced(
    val signaleringId: Long?,
    val signaleringType: SignaleringType.Type,
    val subjecttype: SignaleringSubject,
    val subject: String,
    val tijdstip: ZonedDateTime?
) : LogEvent("Replacing the timestamp of a stored signalering")
