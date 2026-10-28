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
                actorUsername = "fakeUsername"
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

    context("Converting a screen event to a string") {
        given("a screen event") {
            val screenEvent = createScreenEvent(
                opcode = Opcode.UPDATED,
                screenEventType = ScreenEventType.ZAAK,
                screenEventId = createScreenEventId(resource = "fakeResource", detail = "fakeDetail")
            )

            then("the string holds the class name, opcode, type and id") {
                screenEvent.toString() shouldBe "ScreenEvent UPDATED ZAAK fakeResource;fakeDetail"
            }
        }
    }
})
