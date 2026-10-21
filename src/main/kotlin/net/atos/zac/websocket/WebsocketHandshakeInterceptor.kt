/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.websocket

import jakarta.websocket.HandshakeResponse
import jakarta.websocket.server.HandshakeRequest
import jakarta.websocket.server.ServerEndpointConfig

/**
 * This interceptor is only needed to access the httpSession when opening a websocket.
 */
class WebsocketHandshakeInterceptor : ServerEndpointConfig.Configurator() {
    companion object {
        const val HTTP_SESSION = "httpSession"
    }

    override fun modifyHandshake(
        config: ServerEndpointConfig,
        request: HandshakeRequest,
        response: HandshakeResponse
    ) {
        config.userProperties[HTTP_SESSION] = request.httpSession
    }
}
