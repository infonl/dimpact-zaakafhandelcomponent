/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.time

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import jakarta.ws.rs.BadRequestException
import java.time.LocalDate

class LocalDateParamConverterTest : BehaviorSpec({
    val localDateParamConverter = LocalDateParamConverter()

    given("a query parameter with a date in the ISO date format") {
        `when`("the parameter is converted") {
            val localDate = localDateParamConverter.fromString("2025-01-31")

            then("the date is returned") {
                localDate shouldBe LocalDate.of(2025, 1, 31)
            }
        }
    }

    given("a query parameter with a date and a time") {
        `when`("the parameter is converted") {
            val badRequestException = shouldThrow<BadRequestException> {
                localDateParamConverter.fromString("2024-12-31T23:00:00.000Z")
            }

            then("the request is refused, so a time in UTC is never read as the previous day") {
                badRequestException.response.status shouldBe 400
            }
        }
    }

    given("a date") {
        `when`("it is converted to a parameter") {
            val parameter = localDateParamConverter.toString(LocalDate.of(2025, 1, 31))

            then("the parameter has the ISO date format") {
                parameter shouldBe "2025-01-31"
            }
        }
    }
})
