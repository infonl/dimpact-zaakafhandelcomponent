/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.time

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.time.Instant
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.Date

class DateTimeConverterUtilTest : BehaviorSpec({
    extension(UtcDefaultTimeZone())

    given("the JVM time zone UTC and a date at 22:30 UTC on 29-07-2026") {
        val date = Date.from(Instant.parse("2026-07-29T22:30:00Z"))

        `when`("converting to a LocalDate") {
            val localDate = date.toLocalDate()

            then("it returns the date in Europe/Amsterdam") {
                localDate shouldBe LocalDate.of(2026, 7, 30)
            }
        }

        `when`("converting to a ZonedDateTime") {
            val zonedDateTime = date.toZonedDateTime()

            then("it returns the same instant in Europe/Amsterdam") {
                zonedDateTime shouldBe ZonedDateTime.of(2026, 7, 30, 0, 30, 0, 0, ZacTime.ZONE)
            }
        }
    }

    given("the JVM time zone UTC and the LocalDate 30-07-2026") {
        val localDate = LocalDate.of(2026, 7, 30)

        `when`("converting to a Date") {
            val date = localDate.toDate()

            then("it returns the start of that day in Europe/Amsterdam") {
                date shouldBe Date.from(Instant.parse("2026-07-29T22:00:00Z"))
            }
        }
    }
})
