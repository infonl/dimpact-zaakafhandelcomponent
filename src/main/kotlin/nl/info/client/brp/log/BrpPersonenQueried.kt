/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.brp.log

import nl.info.zac.log.LogEvent
import nl.info.zac.log.Outcome

class BrpPersonenQueried(
    val queryType: String?,
    val protocolleringHeaderNames: List<String>,
    val outcome: Outcome,
    val durationMs: Long
) : LogEvent("PersonenApi.personen()")
