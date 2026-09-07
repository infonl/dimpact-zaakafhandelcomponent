/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.websocket

import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.websocket.RemoteEndpoint
import jakarta.websocket.Session
import java.io.IOException

class WebSocketHeartbeatSchedulerTest : BehaviorSpec({
    val sessionRegistry = mockk<SessionRegistry>()
    val webSocketHeartbeatScheduler = WebSocketHeartbeatScheduler(sessionRegistry)

    afterEach {
        checkUnnecessaryStub()
    }

    given("an open WebSocket session") {
        val session = mockk<Session>()
        val basicRemote = mockk<RemoteEndpoint.Basic>(relaxed = true)
        every { sessionRegistry.listAllSessions() } returns setOf(session)
        every { session.isOpen } returns true
        every { session.basicRemote } returns basicRemote

        `when`("heartbeats are sent") {
            webSocketHeartbeatScheduler.sendHeartbeats()

            then("a ping frame is sent to the session") {
                verify(exactly = 1) { basicRemote.sendPing(any()) }
            }
        }
    }

    given("a closed WebSocket session") {
        val session = mockk<Session>()
        every { sessionRegistry.listAllSessions() } returns setOf(session)
        every { session.isOpen } returns false

        `when`("heartbeats are sent") {
            webSocketHeartbeatScheduler.sendHeartbeats()

            then("no ping frame is sent to the session") {
                verify(exactly = 0) { session.basicRemote }
            }
        }
    }

    given("a session that throws an IOException while being sent a ping, alongside a healthy session") {
        val failingSession = mockk<Session>()
        val failingBasicRemote = mockk<RemoteEndpoint.Basic>()
        val healthySession = mockk<Session>()
        val healthyBasicRemote = mockk<RemoteEndpoint.Basic>(relaxed = true)
        every { sessionRegistry.listAllSessions() } returns setOf(failingSession, healthySession)
        every { failingSession.isOpen } returns true
        every { failingSession.basicRemote } returns failingBasicRemote
        every { failingSession.id } returns "failing-session-id"
        every { failingBasicRemote.sendPing(any()) } throws IOException("connection reset")
        every { healthySession.isOpen } returns true
        every { healthySession.basicRemote } returns healthyBasicRemote

        `when`("heartbeats are sent") {
            webSocketHeartbeatScheduler.sendHeartbeats()

            then("the failure is swallowed and the healthy session still receives its ping") {
                verify(exactly = 1) { healthyBasicRemote.sendPing(any()) }
            }
        }
    }
})
