/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.util.time

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDate

class LocalDateAdapterTest : BehaviorSpec({
    val localDateAdapter = LocalDateAdapter()

    given("a null LocalDate") {
        `when`("adapting to JSON") {
            val result = localDateAdapter.adaptToJson(null)

            then("it should return null") {
                result shouldBe null
            }
        }
    }

    given("a LocalDate") {
        val localDate = LocalDate.of(2024, 3, 11)

        `when`("adapting to JSON") {
            val result = localDateAdapter.adaptToJson(localDate)

            then("it should format it as an ISO date") {
                result shouldBe "2024-03-11"
            }
        }
    }

    given("a blank string") {
        `when`("adapting from JSON") {
            val result = localDateAdapter.adaptFromJson("")

            then("it should return null") {
                result shouldBe null
            }
        }
    }

    given("a zoned date-time string") {
        `when`("adapting from JSON") {
            val result = localDateAdapter.adaptFromJson("2024-03-11T10:44:00+01:00")

            then("it should return the local date part") {
                result shouldBe LocalDate.of(2024, 3, 11)
            }
        }
    }

    given("a plain ISO date string") {
        `when`("adapting from JSON") {
            val result = localDateAdapter.adaptFromJson("2024-03-11")

            then("it should parse it directly") {
                result shouldBe LocalDate.of(2024, 3, 11)
            }
        }
    }
})
