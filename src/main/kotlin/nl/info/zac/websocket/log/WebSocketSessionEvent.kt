/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.websocket.log

import nl.info.zac.log.Employee
import nl.info.zac.log.LogEvent

sealed class WebSocketSessionEvent(
    description: String,
    val employee: Employee?,
    val sessionId: String
) : LogEvent(description) {
    class WebSocketError(employee: Employee?, sessionId: String) :
        WebSocketSessionEvent("WebSocket error", employee, sessionId)

    class WebSocketOpened(employee: Employee?, sessionId: String) :
        WebSocketSessionEvent("WebSocket open", employee, sessionId)
}
