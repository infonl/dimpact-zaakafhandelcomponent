/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.websocket

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
import net.atos.zac.websocket.WebsocketHandshakeInterceptor.HTTP_SESSION
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.authentication.LoggedInUserProvider.Companion.LOGGED_IN_USER_SESSION_ATTRIBUTE

class WebSocketServerEndPointTest : BehaviorSpec({
    val registry = mockk<SessionRegistry>()
    val endpoint = WebSocketServerEndPoint(registry)

    beforeEach {
        checkUnnecessaryStub()
    }

    Given("a WebSocket open event with no HTTP session in the endpoint config") {
        val wsSession = mockk<Session>(relaxed = true)
        val endpointConfig = mockk<EndpointConfig>()
        every { endpointConfig.userProperties } returns mutableMapOf()

        When("open is called") {
            endpoint.open(wsSession, endpointConfig)

            Then("access is denied and the WebSocket session is closed with VIOLATED_POLICY") {
                verify(exactly = 1) {
                    wsSession.close(match { it.closeCode.code == CloseReason.CloseCodes.VIOLATED_POLICY.code })
                }
            }
        }
    }

    Given("a WebSocket open event with an HTTP session that has no logged-in user") {
        val wsSession = mockk<Session>(relaxed = true)
        val endpointConfig = mockk<EndpointConfig>()
        val httpSession = mockk<HttpSession>()
        every { endpointConfig.userProperties } returns mutableMapOf<String, Any>(HTTP_SESSION to httpSession)
        every { httpSession.getAttribute(LOGGED_IN_USER_SESSION_ATTRIBUTE) } returns null

        When("open is called") {
            endpoint.open(wsSession, endpointConfig)

            Then("access is denied and the WebSocket session is closed with VIOLATED_POLICY") {
                verify(exactly = 1) {
                    wsSession.close(match { it.closeCode.code == CloseReason.CloseCodes.VIOLATED_POLICY.code })
                }
            }
        }
    }

    Given("a WebSocket open event with an authenticated HTTP session") {
        val wsSession = mockk<Session>(relaxed = true)
        val endpointConfig = mockk<EndpointConfig>()
        val httpSession = mockk<HttpSession>()
        val wsUserProperties = mutableMapOf<String, Any>()
        val loggedInUser = LoggedInUser(
            id = "user-123",
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

        When("open is called") {
            endpoint.open(wsSession, endpointConfig)

            Then("the WebSocket session is opened and the user ID is stored in session properties") {
                wsUserProperties[LOGGED_IN_USER_SESSION_ATTRIBUTE] shouldBe "user-123"
            }
        }
    }

    Given("an open WebSocket session receiving a non-null subscription message") {
        val wsSession = mockk<Session>(relaxed = true)
        val message = SubscriptionType.DELETE_ALL.message()
        every { registry.deleteAll(wsSession) } just runs

        When("the message is processed") {
            endpoint.processMessage(message, wsSession)

            Then("the message is registered with the session registry") {
                verify(exactly = 1) { registry.deleteAll(wsSession) }
            }
        }
    }

    Given("an open WebSocket session receiving a null subscription message") {
        val wsSession = mockk<Session>(relaxed = true)

        When("the message is processed") {
            endpoint.processMessage(null, wsSession)

            Then("no registry operation is performed") {
                verify(exactly = 0) { registry.deleteAll(wsSession) }
            }
        }
    }

    Given("a WebSocket session close event") {
        val wsSession = mockk<Session>(relaxed = true)
        val closeReason = CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, "normal close")
        every { registry.deleteAll(wsSession) } just runs

        When("close is called") {
            endpoint.close(wsSession, closeReason)

            Then("DELETE_ALL is processed for the session to prevent resource leaks") {
                verify(exactly = 1) { registry.deleteAll(wsSession) }
            }
        }
    }

    Given("a WebSocket error event with an exception that has a message") {
        val wsSession = mockk<Session>(relaxed = true)

        When("the error handler is called") {
            endpoint.log(wsSession, RuntimeException("connection reset"))

            Then("the error is logged without throwing") { }
        }
    }

    Given("a WebSocket error event with an exception that has no message") {
        val wsSession = mockk<Session>(relaxed = true)

        When("the error handler is called") {
            endpoint.log(wsSession, object : Exception() {})

            Then("the class simple name is used as log message and no exception is thrown") { }
        }
    }
})
