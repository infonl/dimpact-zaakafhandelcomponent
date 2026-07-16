/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.util.time

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.time.ZonedDateTime

class ZonedDateTimeAdapterTest : BehaviorSpec({
    val zonedDateTimeAdapter = ZonedDateTimeAdapter()

    given("a null ZonedDateTime") {
        `when`("adapting to JSON") {
            val result = zonedDateTimeAdapter.adaptToJson(null)

            then("it should return null") {
                result shouldBe null
            }
        }
    }

    given("a ZonedDateTime") {
        val zonedDateTime = ZonedDateTime.parse("2024-03-11T10:44:00Z")

        `when`("adapting to JSON") {
            val result = zonedDateTimeAdapter.adaptToJson(zonedDateTime)

            then("it should format it as an ISO instant") {
                result shouldBe "2024-03-11T10:44:00Z"
            }
        }
    }

    given("an ISO instant string") {
        `when`("adapting from JSON") {
            val result = zonedDateTimeAdapter.adaptFromJson("2024-03-11T10:44:00Z")

            then("it should parse it into a ZonedDateTime") {
                result shouldBe ZonedDateTime.parse("2024-03-11T10:44:00Z")
            }
        }
    }
})
