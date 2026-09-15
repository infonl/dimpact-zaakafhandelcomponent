/*
 * SPDX-FileCopyrightText: 2021 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.websocket

import jakarta.inject.Inject
import jakarta.servlet.http.HttpSession
import jakarta.websocket.CloseReason
import jakarta.websocket.CloseReason.CloseCodes.VIOLATED_POLICY
import jakarta.websocket.EndpointConfig
import jakarta.websocket.OnClose
import jakarta.websocket.OnError
import jakarta.websocket.OnMessage
import jakarta.websocket.OnOpen
import jakarta.websocket.Session
import jakarta.websocket.server.ServerEndpoint
import net.atos.zac.websocket.SubscriptionType.DELETE_ALL
import net.atos.zac.websocket.SubscriptionType.SubscriptionMessage
import net.atos.zac.websocket.WebsocketHandshakeInterceptor.HTTP_SESSION
import nl.info.zac.authentication.LoggedInUserProvider.Companion.LOGGED_IN_USER_SESSION_ATTRIBUTE
import nl.info.zac.authentication.getLoggedInUser
import java.io.IOException
import java.util.logging.Level
import java.util.logging.Logger

@ServerEndpoint(
    value = "/websocket",
    configurator = WebsocketHandshakeInterceptor::class,
    decoders = [WebSocketSubscriptionMessageDecoder::class]
)
class WebSocketServerEndPoint @Inject constructor(
    private val sessionRegistry: SessionRegistry
) {
    companion object {
        private val LOG = Logger.getLogger(WebSocketServerEndPoint::class.java.name)
    }

    @OnOpen
    fun open(session: Session, conf: EndpointConfig) {
        val httpSession = conf.userProperties[HTTP_SESSION] as? HttpSession
        val loggedInUser = httpSession?.let(::getLoggedInUser)
        if (loggedInUser == null) {
            denyAccess(session, "no logged in user")
        } else {
            session.userProperties[LOGGED_IN_USER_SESSION_ATTRIBUTE] = loggedInUser.id
            sessionRegistry.addSession(session)
            LOG.fine { "WebSocket open for ${user(session)}" }
        }
    }

    @OnMessage
    fun processMessage(message: SubscriptionMessage?, session: Session) {
        if (message != null) {
            LOG.fine { "WebSocket subscription ${message.subscriptionType} for ${user(session)} (${message.event})" }
            message.register(sessionRegistry, session)
        }
    }

    @OnError
    fun log(session: Session, exception: Throwable) {
        val message = exception.message ?: exception.javaClass.simpleName
        LOG.log(Level.INFO, "WebSocket error for ${user(session)} ($message)", exception)
    }

    @OnClose
    fun close(session: Session, reason: CloseReason) {
        LOG.fine { "WebSocket closed for ${user(session)} (${CloseReason.CloseCodes.getCloseCode(reason.closeCode.code)})" }
        sessionRegistry.removeSession(session)
        // Prevent resource leaks by always processing a fictitious DELETE_ALL message when closing.
        processMessage(DELETE_ALL.message(), session)
    }

    private fun denyAccess(session: Session, reason: String) {
        LOG.fine { "Open WebSocket denied for ${user(session)} ($reason)" }
        try {
            // According to the RFC, this close reason should be used if the other reasons are not applicable.
            session.close(CloseReason(VIOLATED_POLICY, reason))
        } catch (ioException: IOException) {
            log(session, ioException)
        }
    }

    private fun user(session: Session): String {
        val medewerker = session.userProperties[LOGGED_IN_USER_SESSION_ATTRIBUTE] as String?
        return if (medewerker != null) "user $medewerker" else "session ${session.id}"
    }
}
