/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.websocket.event

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.comparables.shouldBeGreaterThanOrEqualTo
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import net.atos.zac.event.Opcode
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

class ScreenEventTest : BehaviorSpec({
    context("Comparing screen events") {
        given("two screen events with the same opcode, type and id, but a different id detail and actor") {
            val screenEvent = createScreenEvent(screenEventId = createScreenEventId(detail = "fakeDetail")).apply {
                actorUserId = "fakeUserId"
            }
            val otherScreenEvent = createScreenEvent(screenEventId = createScreenEventId(detail = "otherFakeDetail"))

            then("they are equal and have the same hash code") {
                screenEvent shouldBe otherScreenEvent
                screenEvent.hashCode() shouldBe otherScreenEvent.hashCode()
            }
        }

        given("two screen events with a different opcode") {
            then("they are not equal") {
                createScreenEvent(opcode = Opcode.UPDATED) shouldNotBe createScreenEvent(opcode = Opcode.DELETED)
            }
        }

        given("two screen events with a different type") {
            then("they are not equal") {
                createScreenEvent(screenEventType = ScreenEventType.ZAAK) shouldNotBe
                    createScreenEvent(screenEventType = ScreenEventType.TAAK)
            }
        }

        given("two screen events with a different id") {
            then("they are not equal") {
                createScreenEvent(screenEventId = createScreenEventId(resource = "fakeResource")) shouldNotBe
                    createScreenEvent(screenEventId = createScreenEventId(resource = "otherFakeResource"))
            }
        }

        given("a screen event") {
            val screenEvent = createScreenEvent()
            val missingObject: Any? = null

            then("it is equal to itself, but not to null or to an object of another class") {
                screenEvent shouldBe screenEvent
                screenEvent.equals(missingObject) shouldBe false
                screenEvent.equals("fakeString") shouldBe false
            }
        }
    }

    context("Delaying a screen event") {
        given("a screen event without a delay") {
            val screenEvent = createScreenEvent()

            `when`("the event is delayed") {
                val startTime = System.nanoTime()
                screenEvent.delay()

                then("it returns at once") {
                    (System.nanoTime() - startTime).nanoseconds shouldBeLessThan 1.seconds
                }
            }
        }

        given("a screen event with a delay of one second") {
            val screenEvent = createScreenEvent().apply { setDelay(1) }

            `when`("the event is delayed") {
                val startTime = System.nanoTime()
                screenEvent.delay()

                then("it waits for that second") {
                    (System.nanoTime() - startTime).nanoseconds shouldBeGreaterThanOrEqualTo 1.seconds
                }
            }
        }

        given("a screen event with a delay and a thread that is interrupted") {
            val screenEvent = createScreenEvent().apply { setDelay(10) }

            `when`("the event is delayed") {
                val startTime = System.nanoTime()
                Thread.currentThread().interrupt()
                screenEvent.delay()
                val isInterrupted = Thread.interrupted()

                then("it stops waiting at once and keeps the interrupted status of the thread") {
                    (System.nanoTime() - startTime).nanoseconds shouldBeLessThan 1.seconds
                    isInterrupted shouldBe true
                }
            }
        }
    }

    context("Converting a screen event to a string") {
        given("a screen event without a delay") {
            val screenEvent = createScreenEvent(
                opcode = Opcode.UPDATED,
                screenEventType = ScreenEventType.ZAAK,
                screenEventId = createScreenEventId(resource = "fakeResource", detail = "fakeDetail")
            )

            then("the string holds the class name, opcode, type and id") {
                screenEvent.toString() shouldBe "ScreenEvent UPDATED ZAAK fakeResource;fakeDetail"
            }
        }

        given("a screen event with a delay") {
            val screenEvent = createScreenEvent(
                opcode = Opcode.UPDATED,
                screenEventType = ScreenEventType.ZAAK,
                screenEventId = createScreenEventId(resource = "fakeResource", detail = "fakeDetail")
            ).apply { setDelay(3) }

            then("the class name in the string is followed by the delay in seconds") {
                screenEvent.toString() shouldBe "ScreenEvent+3s UPDATED ZAAK fakeResource;fakeDetail"
            }
        }
    }
})
