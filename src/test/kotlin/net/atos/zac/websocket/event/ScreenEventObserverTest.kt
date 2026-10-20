/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.websocket.event

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import jakarta.websocket.RemoteEndpoint
import jakarta.websocket.Session
import net.atos.zac.event.Opcode
import net.atos.zac.websocket.SessionRegistry

class ScreenEventObserverTest : BehaviorSpec({
    val sessionRegistry = mockk<SessionRegistry>()
    val screenEventObserver = ScreenEventObserver(sessionRegistry)
    val objectMapper = ObjectMapper()

    afterEach {
        checkUnnecessaryStub()
    }

    context("Handling a screen event") {
        given("a screen event to which a session has subscribed") {
            val screenEvent = createScreenEvent(
                opcode = Opcode.UPDATED,
                screenEventType = ScreenEventType.ZAAK,
                screenEventId = createScreenEventId(resource = "fakeResource", detail = "fakeDetail")
            ).apply { actorUserId = "fakeUserId" }
            val session = mockk<Session>()
            val asyncRemoteEndpoint = mockk<RemoteEndpoint.Async>()
            val jsonSlot = slot<String>()
            every { sessionRegistry.listSessions(screenEvent) } returns setOf(session)
            every { session.asyncRemote } returns asyncRemoteEndpoint
            every { asyncRemoteEndpoint.sendText(capture(jsonSlot)) } returns mockk()

            `when`("the event is fired") {
                screenEventObserver.onFire(screenEvent)

                then("the event is sent to the session as JSON, without its delay") {
                    verify(exactly = 1) { asyncRemoteEndpoint.sendText(any()) }
                    objectMapper.readTree(jsonSlot.captured) shouldBe objectMapper.readTree(
                        """
                        {
                            "timestamp": ${screenEvent.timestamp},
                            "opcode": "UPDATED",
                            "objectType": "ZAAK",
                            "objectId": { "resource": "fakeResource", "detail": "fakeDetail" },
                            "actorUserId": "fakeUserId"
                        }
                        """.trimIndent()
                    )
                }
            }
        }

        given("a screen event to which no session has subscribed") {
            val screenEvent = createScreenEvent()
            every { sessionRegistry.listSessions(screenEvent) } returns emptySet()

            `when`("the event is fired") {
                screenEventObserver.onFire(screenEvent)

                then("nothing is sent") {
                    verify(exactly = 1) { sessionRegistry.listSessions(screenEvent) }
                }
            }
        }
    }
})
