/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.websocket.log

import nl.info.zac.log.Employee
import nl.info.zac.log.LogEvent

class WebSocketDenied(
    val reason: String,
    val employee: Employee?,
    val sessionId: String
) : LogEvent("Open WebSocket denied")
