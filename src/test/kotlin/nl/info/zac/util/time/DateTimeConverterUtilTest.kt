/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.util.time

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Date

class DateTimeConverterUtilTest : BehaviorSpec({

    given("a date") {
        val date = Date.from(LocalDate.of(2024, 3, 11).atStartOfDay(ZoneId.systemDefault()).toInstant())

        `when`("converting to a LocalDate") {
            val localDate = date.toLocalDate()

            then("it should return the corresponding LocalDate") {
                localDate shouldBe LocalDate.of(2024, 3, 11)
            }
        }

        `when`("converting to a ZonedDateTime") {
            val zonedDateTime = date.toZonedDateTime()

            then("it should return the corresponding ZonedDateTime in the system default time zone") {
                zonedDateTime shouldBe ZonedDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault())
            }
        }
    }

    given("a LocalDate") {
        val localDate = LocalDate.of(2024, 3, 11)

        `when`("converting to a Date") {
            val date = localDate.toDate()

            then("it should return the corresponding Date at start of day in the system default time zone") {
                date shouldBe Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant())
            }
        }
    }
})
