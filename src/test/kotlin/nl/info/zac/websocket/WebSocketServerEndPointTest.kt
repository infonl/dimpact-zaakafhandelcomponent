/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.websocket

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import jakarta.servlet.http.HttpSession
import jakarta.websocket.CloseReason
import jakarta.websocket.EndpointConfig
import jakarta.websocket.Session
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.authentication.LoggedInUserProvider.Companion.LOGGED_IN_USER_SESSION_ATTRIBUTE
import nl.info.zac.event.Opcode
import nl.info.zac.log.EmployeeKeycloakUserIds
import nl.info.zac.log.LogCapture
import nl.info.zac.log.ZacLogger
import nl.info.zac.websocket.WebsocketHandshakeInterceptor.Companion.HTTP_SESSION
import nl.info.zac.websocket.event.ScreenEventType
import nl.info.zac.websocket.event.createScreenEvent
import nl.info.zac.websocket.event.createScreenEventId

class WebSocketServerEndPointTest : BehaviorSpec({
    val sessionRegistry = mockk<SessionRegistry>()
    val endpoint = WebSocketServerEndPoint(sessionRegistry)

    afterEach {
        checkUnnecessaryStub()
    }

    afterSpec {
        EmployeeKeycloakUserIds.reset()
    }

    given("a WebSocket open event with no HTTP session in the endpoint config") {
        val wsSession = mockk<Session>(relaxed = true)
        val endpointConfig = mockk<EndpointConfig>()
        every { endpointConfig.userProperties } returns mutableMapOf()
        every { wsSession.id } returns "fakeSessionId"

        `when`("open is called") {
            val records = LogCapture(WebSocketServerEndPoint::class).use { logCapture ->
                endpoint.open(wsSession, endpointConfig)
                logCapture.records
            }

            then("access is denied and the WebSocket session is closed with VIOLATED_POLICY") {
                verify(exactly = 1) {
                    wsSession.close(match { it.closeCode.code == CloseReason.CloseCodes.VIOLATED_POLICY.code })
                }
            }

            then("the denial is logged with the session ID and without an employee") {
                with(records.single()) {
                    message shouldBe "Open WebSocket denied"
                    mdc["sessionId"] shouldBe "fakeSessionId"
                    mdc["employee"] shouldBe "null"
                }
            }
        }
    }

    given("a WebSocket open event where the HTTP session property has an unexpected type") {
        val wsSession = mockk<Session>(relaxed = true)
        val endpointConfig = mockk<EndpointConfig>()
        every { endpointConfig.userProperties } returns mutableMapOf<String, Any>(HTTP_SESSION to "not an HttpSession")

        `when`("open is called") {
            endpoint.open(wsSession, endpointConfig)

            then("access is denied and the WebSocket session is closed with VIOLATED_POLICY, without throwing") {
                verify(exactly = 1) {
                    wsSession.close(match { it.closeCode.code == CloseReason.CloseCodes.VIOLATED_POLICY.code })
                }
            }
        }
    }

    given("a WebSocket open event with an HTTP session that has no logged-in user") {
        val wsSession = mockk<Session>(relaxed = true)
        val endpointConfig = mockk<EndpointConfig>()
        val httpSession = mockk<HttpSession>()
        every { endpointConfig.userProperties } returns mutableMapOf<String, Any>(HTTP_SESSION to httpSession)
        every { httpSession.getAttribute(LOGGED_IN_USER_SESSION_ATTRIBUTE) } returns null

        `when`("open is called") {
            endpoint.open(wsSession, endpointConfig)

            then("access is denied and the WebSocket session is closed with VIOLATED_POLICY") {
                verify(exactly = 1) {
                    wsSession.close(match { it.closeCode.code == CloseReason.CloseCodes.VIOLATED_POLICY.code })
                }
            }
        }
    }

    given("a WebSocket open event with an invalidated HTTP session") {
        val wsSession = mockk<Session>(relaxed = true)
        val endpointConfig = mockk<EndpointConfig>()
        val httpSession = mockk<HttpSession>()
        every { endpointConfig.userProperties } returns mutableMapOf<String, Any>(HTTP_SESSION to httpSession)
        every { httpSession.getAttribute(LOGGED_IN_USER_SESSION_ATTRIBUTE) } throws IllegalStateException("session invalidated")

        `when`("open is called") {
            endpoint.open(wsSession, endpointConfig)

            then("access is denied and the WebSocket session is closed with VIOLATED_POLICY") {
                verify(exactly = 1) {
                    wsSession.close(match { it.closeCode.code == CloseReason.CloseCodes.VIOLATED_POLICY.code })
                }
            }
        }
    }

    given("a WebSocket open event with an authenticated HTTP session") {
        val wsSession = mockk<Session>(relaxed = true)
        val endpointConfig = mockk<EndpointConfig>()
        val httpSession = mockk<HttpSession>()
        val wsUserProperties = mutableMapOf<String, Any>()
        val loggedInUser = LoggedInUser(
            username = "user-123",
            firstName = "Test",
            lastName = "User",
            displayName = null,
            email = null,
            roles = emptySet(),
            groupIds = emptySet()
        )
        every { wsSession.userProperties } returns wsUserProperties
        every { endpointConfig.userProperties } returns mutableMapOf<String, Any>(HTTP_SESSION to httpSession)
        every { httpSession.getAttribute(LOGGED_IN_USER_SESSION_ATTRIBUTE) } returns loggedInUser
        every { sessionRegistry.addSession(wsSession) } just runs
        ZacLogger.registerEmployee("user-123", "fakeKeycloakUserId")

        `when`("open is called") {
            val records = LogCapture(WebSocketServerEndPoint::class).use { logCapture ->
                endpoint.open(wsSession, endpointConfig)
                logCapture.records
            }

            then("the WebSocket session is opened and the user ID is stored in session properties") {
                wsUserProperties[LOGGED_IN_USER_SESSION_ATTRIBUTE] shouldBe "user-123"
            }

            then("the session is registered in the session registry so it can receive heartbeats") {
                verify(exactly = 1) { sessionRegistry.addSession(wsSession) }
            }

            then("the opening is logged with the Keycloak user ID of the employee and not the username") {
                with(records.single()) {
                    message shouldBe "WebSocket open"
                    mdc["employee"] shouldBe "fakeKeycloakUserId"
                    containsAnywhere("user-123") shouldBe false
                }
            }
        }
    }

    given("an open WebSocket session receiving a non-null subscription message") {
        val wsSession = mockk<Session>(relaxed = true)
        val message = SubscriptionType.DELETE_ALL.message()
        every { sessionRegistry.deleteAll(wsSession) } just runs

        `when`("the message is processed") {
            endpoint.processMessage(message, wsSession)

            then("the message is registered with the session registry") {
                verify(exactly = 1) { sessionRegistry.deleteAll(wsSession) }
            }
        }
    }

    given("an open WebSocket session subscribing to the signaleringen of a user, whose object ID is the username") {
        val wsSession = mockk<Session>(relaxed = true)
        val screenEvent = createScreenEvent(
            opcode = Opcode.UPDATED,
            screenEventType = ScreenEventType.SIGNALERINGEN,
            screenEventId = createScreenEventId(resource = "fakeSubscribedUsername")
        )
        every { sessionRegistry.create(screenEvent, wsSession) } just runs

        `when`("the message is processed") {
            val records = LogCapture(WebSocketServerEndPoint::class).use { logCapture ->
                endpoint.processMessage(SubscriptionType.CREATE.message(screenEvent), wsSession)
                logCapture.records
            }

            then("the subscription is logged with the type of the screen event, but not with the username in its ID") {
                with(records.single()) {
                    mdc["opcode"] shouldBe "UPDATED"
                    mdc["screenEventType"] shouldBe "SIGNALERINGEN"
                    containsAnywhere("fakeSubscribedUsername") shouldBe false
                }
            }
        }
    }

    given("an open WebSocket session receiving a null subscription message") {
        val wsSession = mockk<Session>(relaxed = true)

        `when`("the message is processed") {
            endpoint.processMessage(null, wsSession)

            then("no sessionRegistry operation is performed") {
                verify(exactly = 0) { sessionRegistry.deleteAll(wsSession) }
            }
        }
    }

    given("a WebSocket session close event") {
        val wsSession = mockk<Session>(relaxed = true)
        val closeReason = CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, "normal close")
        every { sessionRegistry.deleteAll(wsSession) } just runs
        every { sessionRegistry.removeSession(wsSession) } just runs
        every { wsSession.userProperties } returns mutableMapOf<String, Any>(
            LOGGED_IN_USER_SESSION_ATTRIBUTE to "fakeClosingUsername"
        )
        ZacLogger.registerEmployee("fakeClosingUsername", "fakeClosingKeycloakUserId")

        `when`("close is called") {
            val records = LogCapture(WebSocketServerEndPoint::class).use { logCapture ->
                endpoint.close(wsSession, closeReason)
                logCapture.records
            }

            then("the closing is logged with the close code and the Keycloak user ID of the employee") {
                with(records.first()) {
                    message shouldBe "WebSocket closed"
                    mdc["closeCode"] shouldBe "NORMAL_CLOSURE"
                    mdc["employee"] shouldBe "fakeClosingKeycloakUserId"
                    containsAnywhere("fakeClosingUsername") shouldBe false
                }
            }

            then("the DELETE_ALL subscription that the closing processes is logged without a screen event") {
                with(records.last()) {
                    mdc["subscriptionType"] shouldBe "DELETE_ALL"
                    mdc["opcode"] shouldBe "null"
                    mdc["screenEventType"] shouldBe "null"
                }
            }

            then("DELETE_ALL is processed for the session to prevent resource leaks") {
                verify(exactly = 1) { sessionRegistry.deleteAll(wsSession) }
            }

            then("the session is removed from the session registry so it no longer receives heartbeats") {
                verify(exactly = 1) { sessionRegistry.removeSession(wsSession) }
            }
        }
    }

    given("a WebSocket error event with an exception that has a message") {
        val wsSession = mockk<Session>(relaxed = true)

        `when`("the error handler is called") {
            endpoint.log(wsSession, RuntimeException("connection reset"))

            then("the error is logged without throwing") { }
        }
    }

    given("a WebSocket error event with an exception that has no message") {
        val wsSession = mockk<Session>(relaxed = true)

        `when`("the error handler is called") {
            endpoint.log(wsSession, object : Exception() {})

            then("the class simple name is used as log message and no exception is thrown") { }
        }
    }
})
