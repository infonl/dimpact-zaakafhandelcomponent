/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.websocket.log

import nl.info.zac.log.Employee
import nl.info.zac.log.LogEvent

class WebSocketSessionEvent(
    val type: Type,
    val employee: Employee?,
    val sessionId: String
) : LogEvent(type.description) {
    enum class Type(val description: String) {
        WEB_SOCKET_ERROR("WebSocket error"),
        WEB_SOCKET_OPENED("WebSocket open")
    }
}
