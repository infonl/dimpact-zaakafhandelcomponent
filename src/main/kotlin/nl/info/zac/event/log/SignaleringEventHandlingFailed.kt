/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.event.log

import nl.info.zac.log.LogEvent

class SignaleringEventHandlingFailed(
    val opcode: String,
    val signaleringType: String,
    val objectId: String
) : LogEvent("Failed to handle signalering event")
