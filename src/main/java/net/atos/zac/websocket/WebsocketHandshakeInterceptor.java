/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.websocket;

import jakarta.servlet.http.HttpSession;
import jakarta.websocket.HandshakeResponse;
import jakarta.websocket.server.HandshakeRequest;
import jakarta.websocket.server.ServerEndpointConfig;

/**
 * This interceptor is only needed to access the httpSession when opening a websocket.
 */
public class WebsocketHandshakeInterceptor extends ServerEndpointConfig.Configurator {

    public static final String HTTP_SESSION = "httpSession";

    @Override
    public void modifyHandshake(ServerEndpointConfig config, HandshakeRequest request, HandshakeResponse response) {
        final HttpSession httpSession = (HttpSession) request.getHttpSession();
        config.getUserProperties().put(HTTP_SESSION, httpSession);
    }
}
