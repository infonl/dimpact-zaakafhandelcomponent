/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.websocket

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import net.atos.zac.websocket.event.ScreenEventType
import net.atos.zac.websocket.event.createScreenEvent
import javax.websocket.Session

class SessionRegistryTest : BehaviorSpec({
    val session = mockk<Session>()

    given("a new session registry is created and a websocket session exists") {
        When("create is invoked with a new screen event") {
            then("a session for a specific event is added to the session registry") {
                val sessionRegistry = SessionRegistry()
                val screenEvent = createScreenEvent()

                sessionRegistry.create(screenEvent, session)

                sessionRegistry.listSessions(screenEvent).size shouldBe 1
                sessionRegistry.listSessions(screenEvent) shouldContain session
                // check that session registry does not contain a random other screen event
                sessionRegistry.listSessions(createScreenEvent(screenEventType = ScreenEventType.TAAK)).size shouldBe 0
            }
        }
    }
})
