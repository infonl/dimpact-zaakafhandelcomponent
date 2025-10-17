/*
 * SPDX-FileCopyrightText: 2023 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.websocket

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.mockk
import jakarta.websocket.Session
import net.atos.zac.event.Opcode
import net.atos.zac.websocket.event.ScreenEventType
import net.atos.zac.websocket.event.createScreenEvent

class SessionRegistryTest : BehaviorSpec({
    val session1 = mockk<Session>()
    val session2 = mockk<Session>()
    val screenEventCreatedZaak = createScreenEvent(opcode = Opcode.CREATED, screenEventType = ScreenEventType.ZAAK)
    val screenEventAnyAny = createScreenEvent(opcode = Opcode.ANY, screenEventType = ScreenEventType.ANY)

    beforeEach {
        checkUnnecessaryStub()
    }

    Given("a new session registry is created") {
        When("create is invoked with a new screen event of type CREATED") {
            Then("a session for this event is added to the session registry") {
                val sessionRegistry = SessionRegistry()

                sessionRegistry.create(screenEventCreatedZaak, session1)

                sessionRegistry.listSessions(screenEventCreatedZaak).size shouldBe 1
                sessionRegistry.listSessions(screenEventCreatedZaak) shouldContain session1
                // check that session registry does not contain a random other screen event
                sessionRegistry.listSessions(createScreenEvent(screenEventType = ScreenEventType.TAAK)).size shouldBe 0
            }
        }
        When("create is invoked with a new screen event of type ANY") {
            Then("a session for this event is _not_ added to the session registry") {
                val sessionRegistry = SessionRegistry()
                sessionRegistry.create(screenEventAnyAny, session1)

                sessionRegistry.listSessions(screenEventAnyAny).size shouldBe 0
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
    Given("a number of events are added to the session registry for a session") {
        When("delete is invoke for this session and a specific event") {
            Then("all registered events for this session are removed from the registry") {
                val sessionRegistry = SessionRegistry()
                sessionRegistry.create(screenEventCreatedZaak, session1)
                sessionRegistry.create(screenEventAnyAny, session1)

                sessionRegistry.delete(screenEventCreatedZaak, session1)

                sessionRegistry.listSessions(screenEventCreatedZaak).size shouldBe 0
                // all events seem to be removed for this session; not sure if this is intended behavior..
                sessionRegistry.listSessions(screenEventAnyAny).size shouldBe 0
            }
        }
        When("deleteAll is invoke for this session") {
            Then("all registered events for this session are removed from the registry") {
                val sessionRegistry = SessionRegistry()
                sessionRegistry.create(screenEventCreatedZaak, session1)
                sessionRegistry.create(screenEventAnyAny, session1)

                sessionRegistry.deleteAll(session1)

                sessionRegistry.listSessions(screenEventCreatedZaak).size shouldBe 0
                sessionRegistry.listSessions(screenEventAnyAny).size shouldBe 0
            }
        }
    }
    Given("a number of events are added to the session registry for multiple sessions") {
        When("delete is invoke for one session and a specific event") {
            Then("all registered events for this session are removed from the registry") {
                val sessionRegistry = SessionRegistry()
                sessionRegistry.create(screenEventCreatedZaak, session1)
                sessionRegistry.create(screenEventCreatedZaak, session2)
                sessionRegistry.create(screenEventAnyAny, session1)

                sessionRegistry.delete(screenEventCreatedZaak, session1)

                sessionRegistry.listSessions(screenEventCreatedZaak).size shouldBe 1
                // all events seem to be removed for this session; not sure if this is intended behavior..
                sessionRegistry.listSessions(screenEventAnyAny).size shouldBe 0
            }
        }
        When("deleteAll is invoke for one session") {
            Then("all registered events for this session are removed from the registry") {
                val sessionRegistry = SessionRegistry()
                sessionRegistry.create(screenEventCreatedZaak, session1)
                sessionRegistry.create(screenEventCreatedZaak, session2)
                sessionRegistry.create(screenEventAnyAny, session1)

                sessionRegistry.deleteAll(session1)

                sessionRegistry.listSessions(screenEventCreatedZaak).size shouldBe 1
                sessionRegistry.listSessions(screenEventAnyAny).size shouldBe 0
            }
        }
    }
})
