/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.websocket.log

import nl.info.zac.log.LogEvent

class WebSocketHeartbeatFailed(
    val sessionId: String
) : LogEvent("Failed to send WebSocket heartbeat")
