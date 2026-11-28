/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.signalering.log

import nl.info.zac.log.LogEvent
import nl.info.zac.signalering.model.SignaleringSubject
import nl.info.zac.signalering.model.SignaleringType

class SignaleringenListed(
    val subjecttype: SignaleringSubject,
    val signaleringType: SignaleringType.Type
) : LogEvent("Listed signaleringen")
