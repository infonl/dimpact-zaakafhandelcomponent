/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.util.log

import nl.info.zac.log.LogEvent
import nl.info.zac.log.Outcome

@Suppress("LongParameterList")
class OutboundRestCall(
    val correlationId: String,
    val method: String,
    val host: String?,
    val operation: String?,
    val status: Int,
    val outcome: Outcome,
    val durationMs: Long
) : LogEvent("Outbound REST call")
