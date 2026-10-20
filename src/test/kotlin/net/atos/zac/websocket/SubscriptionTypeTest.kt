/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.websocket

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import jakarta.websocket.Session
import net.atos.zac.websocket.event.createScreenEvent

class SubscriptionTypeTest : BehaviorSpec({
    val sessionRegistry = mockk<SessionRegistry>()
    val session = mockk<Session>()

    afterEach {
        checkUnnecessaryStub()
    }

    context("Creating and registering a subscription message") {
        given("a create subscription type and a screen event") {
            val screenEvent = createScreenEvent()
            every { sessionRegistry.create(screenEvent, session) } just runs

            `when`("a message is created for the event and registered") {
                val subscriptionMessage = SubscriptionType.CREATE.message(screenEvent)
                subscriptionMessage.register(sessionRegistry, session)

                then("the message holds the type and the event, and the session subscribes to the event") {
                    subscriptionMessage.subscriptionType shouldBe SubscriptionType.CREATE
                    subscriptionMessage.event shouldBe screenEvent
                    verify(exactly = 1) { sessionRegistry.create(screenEvent, session) }
                }
            }
        }

        given("a delete subscription type and a screen event") {
            val screenEvent = createScreenEvent()
            every { sessionRegistry.delete(screenEvent, session) } just runs

            `when`("a message is created for the event and registered") {
                SubscriptionType.DELETE.message(screenEvent).register(sessionRegistry, session)

                then("the session unsubscribes from the event") {
                    verify(exactly = 1) { sessionRegistry.delete(screenEvent, session) }
                }
            }
        }

        given("a delete all subscription type") {
            every { sessionRegistry.deleteAll(session) } just runs

            `when`("a message is created and registered") {
                val subscriptionMessage = SubscriptionType.DELETE_ALL.message()
                subscriptionMessage.register(sessionRegistry, session)

                then("every message is the same instance without an event, and the session unsubscribes from all events") {
                    subscriptionMessage shouldBeSameInstanceAs SubscriptionType.DELETE_ALL.message(null)
                    subscriptionMessage.event shouldBe null
                    verify(exactly = 1) { sessionRegistry.deleteAll(session) }
                }
            }
        }

        given("a delete all subscription type and a screen event") {
            `when`("a message is created for the event") {
                val illegalArgumentException = shouldThrow<IllegalArgumentException> {
                    SubscriptionType.DELETE_ALL.message(createScreenEvent())
                }

                then("it fails, because a delete all message does not refer to an event") {
                    illegalArgumentException.message shouldBe "Unexpected event argument"
                }
            }
        }

        given("a create subscription type and no screen event") {
            `when`("a message is created") {
                val illegalArgumentException = shouldThrow<IllegalArgumentException> { SubscriptionType.CREATE.message(null) }

                then("it fails, because a create message needs an event") {
                    illegalArgumentException.message shouldBe "Missing event argument"
                }
            }
        }
    }
})
