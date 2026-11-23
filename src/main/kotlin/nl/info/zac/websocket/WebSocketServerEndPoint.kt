/*
 * SPDX-FileCopyrightText: 2021 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.websocket

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
import nl.info.zac.authentication.LoggedInUserProvider.Companion.LOGGED_IN_USER_SESSION_ATTRIBUTE
import nl.info.zac.authentication.getLoggedInUser
import nl.info.zac.log.Employee
import nl.info.zac.log.ZacLogger
import nl.info.zac.websocket.SubscriptionType.DELETE_ALL
import nl.info.zac.websocket.SubscriptionType.SubscriptionMessage
import nl.info.zac.websocket.WebsocketHandshakeInterceptor.Companion.HTTP_SESSION
import nl.info.zac.websocket.log.WebSocketClosed
import nl.info.zac.websocket.log.WebSocketDenied
import nl.info.zac.websocket.log.WebSocketSessionEvent
import nl.info.zac.websocket.log.WebSocketSubscription
import java.io.IOException

@ServerEndpoint(
    value = "/websocket",
    configurator = WebsocketHandshakeInterceptor::class,
    decoders = [WebSocketSubscriptionMessageDecoder::class]
)
class WebSocketServerEndPoint @Inject constructor(
    private val sessionRegistry: SessionRegistry
) {
    companion object {
        private val LOG = ZacLogger.getLogger(WebSocketServerEndPoint::class)
    }

    @OnOpen
    fun open(session: Session, conf: EndpointConfig) {
        val httpSession = conf.userProperties[HTTP_SESSION] as? HttpSession
        val loggedInUser = httpSession?.let(::getLoggedInUser)
        if (loggedInUser == null) {
            denyAccess(session, "no logged in user")
        } else {
            session.userProperties[LOGGED_IN_USER_SESSION_ATTRIBUTE] = loggedInUser.username
            sessionRegistry.addSession(session)
            LOG.fine {
                WebSocketSessionEvent(
                    type = WebSocketSessionEvent.Type.WEB_SOCKET_OPENED,
                    employee = session.employee(),
                    sessionId = session.id
                )
            }
        }
    }

    @OnMessage
    fun processMessage(message: SubscriptionMessage?, session: Session) {
        if (message != null) {
            LOG.fine {
                WebSocketSubscription(
                    subscriptionType = message.subscriptionType.toString(),
                    // Not the object ID: for some screen events, such as SIGNALERINGEN, it is a username
                    opcode = message.event?.opcode?.toString(),
                    screenEventType = message.event?.objectType?.toString(),
                    employee = session.employee(),
                    sessionId = session.id
                )
            }
            message.register(sessionRegistry, session)
        }
    }

    @OnError
    fun log(session: Session, exception: Throwable) {
        LOG.info(
            WebSocketSessionEvent(
                type = WebSocketSessionEvent.Type.WEB_SOCKET_ERROR,
                employee = session.employee(),
                sessionId = session.id
            ),
            exception
        )
    }

    @OnClose
    fun close(session: Session, reason: CloseReason) {
        LOG.fine {
            WebSocketClosed(
                closeCode = CloseReason.CloseCodes.getCloseCode(reason.closeCode.code).toString(),
                employee = session.employee(),
                sessionId = session.id
            )
        }
        sessionRegistry.removeSession(session)
        // Prevent resource leaks by always processing a fictitious DELETE_ALL message when closing.
        processMessage(DELETE_ALL.message(), session)
    }

    private fun denyAccess(session: Session, reason: String) {
        LOG.fine { WebSocketDenied(reason = reason, employee = session.employee(), sessionId = session.id) }
        try {
            // According to the RFC, this close reason should be used if the other reasons are not applicable.
            session.close(CloseReason(VIOLATED_POLICY, reason))
        } catch (ioException: IOException) {
            log(session, ioException)
        }
    }

    private fun Session.employee() = (userProperties[LOGGED_IN_USER_SESSION_ATTRIBUTE] as String?)?.let(::Employee)
}
