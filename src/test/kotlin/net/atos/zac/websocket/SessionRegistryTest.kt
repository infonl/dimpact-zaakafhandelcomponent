/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.websocket

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import net.atos.zac.event.Opcode
import net.atos.zac.websocket.event.ScreenEventType
import net.atos.zac.websocket.event.createScreenEvent
import javax.websocket.Session

class SessionRegistryTest : BehaviorSpec({
    val session = mockk<Session>()

    given("a new session registry is created and a websocket session exists") {
        When("create is invoked with a new screen event of type CREATED") {
            then("a session for this event is added to the session registry") {
                val sessionRegistry = SessionRegistry()
                val screenEvent = createScreenEvent(opcode = Opcode.CREATED)

                sessionRegistry.create(screenEvent, session)

                sessionRegistry.listSessions(screenEvent).size shouldBe 1
                sessionRegistry.listSessions(screenEvent) shouldContain session
                // check that session registry does not contain a random other screen event
                sessionRegistry.listSessions(createScreenEvent(screenEventType = ScreenEventType.TAAK)).size shouldBe 0
            }
        }
        When("create is invoked with a new screen event of type ANY") {
            then("a session for this event is _not_ added to the session registry") {
                val sessionRegistry = SessionRegistry()
                val screenEvent = createScreenEvent(opcode = Opcode.ANY, screenEventType = ScreenEventType.ANY)

                sessionRegistry.create(screenEvent, session)

                sessionRegistry.listSessions(screenEvent).size shouldBe 0
                // however screen events with opcode 'UPDATED' and screen events with opcode 'DELETED'
                // with all available screen types except 'ANY' should have been added to the registry
                ScreenEventType.entries
                    .filter { screenEventType -> screenEventType != ScreenEventType.ANY }
                    .forEach { screenEventType ->
                        sessionRegistry.listSessions(
                            createScreenEvent(opcode = Opcode.UPDATED, screenEventType = screenEventType)
                        ).size shouldBe 1
                        sessionRegistry.listSessions(
                            createScreenEvent(opcode = Opcode.DELETED, screenEventType = screenEventType)
                        ).size shouldBe 1
                    }
            }
        }
    }
})
