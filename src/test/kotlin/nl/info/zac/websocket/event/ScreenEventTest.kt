/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.websocket.event

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import nl.info.zac.event.Opcode

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
