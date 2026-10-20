/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.websocket.event

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class ScreenEventIdTest : BehaviorSpec({
    context("Comparing screen event ids") {
        given("two screen event ids with the same resource and a different detail") {
            val screenEventId = ScreenEventId("fakeResource", "fakeDetail")
            val otherScreenEventId = ScreenEventId("fakeResource", "otherFakeDetail")

            then("they are equal and have the same hash code, because only the resource identifies them") {
                screenEventId shouldBe otherScreenEventId
                screenEventId.hashCode() shouldBe otherScreenEventId.hashCode()
            }
        }

        given("two screen event ids with a different resource") {
            then("they are not equal") {
                ScreenEventId("fakeResource", null) shouldNotBe ScreenEventId("otherFakeResource", null)
            }
        }
    }

    context("Converting a screen event id to a string") {
        given("a screen event id with a detail") {
            then("the string holds the resource and the detail, separated by a semicolon") {
                ScreenEventId("fakeResource", "fakeDetail").toString() shouldBe "fakeResource;fakeDetail"
            }
        }

        given("a screen event id without a detail") {
            then("the string holds only the resource") {
                ScreenEventId("fakeResource", null).toString() shouldBe "fakeResource"
            }
        }
    }
})
