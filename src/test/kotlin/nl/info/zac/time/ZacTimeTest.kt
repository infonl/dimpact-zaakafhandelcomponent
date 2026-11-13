/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.time

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.ZonedDateTime

class ZacTimeTest : BehaviorSpec({
    extension(UtcDefaultTimeZone())

    given("the JVM time zone UTC and a clock in UTC at 23:30 UTC on 29-07-2026") {
        val clock = Clock.fixed(Instant.parse("2026-07-29T23:30:00Z"), ZoneOffset.UTC)

        `when`("today is determined") {
            val today = ZacTime.today(clock)

            then("it is 30-07-2026, the date in Europe/Amsterdam") {
                today shouldBe LocalDate.of(2026, 7, 30)
            }
        }

        `when`("now is determined") {
            val now = ZacTime.now(clock)

            then("it is 01:30 on 30-07-2026 in Europe/Amsterdam") {
                now shouldBe ZonedDateTime.of(2026, 7, 30, 1, 30, 0, 0, ZacTime.ZONE)
            }
        }
    }

    given("the JVM time zone UTC and the date 30-07-2026") {
        val date = LocalDate.of(2026, 7, 30)

        `when`("the start of that day is determined") {
            val startOfDay = ZacTime.startOfDay(date)

            then("it is 00:00 in Europe/Amsterdam, which is 22:00 UTC on 29-07-2026") {
                startOfDay shouldBe ZonedDateTime.of(2026, 7, 30, 0, 0, 0, 0, ZacTime.ZONE)
                startOfDay.toInstant() shouldBe Instant.parse("2026-07-29T22:00:00Z")
            }
        }
    }

    given("the time zone of ZAC") {
        then("it is Europe/Amsterdam") {
            ZacTime.ZONE.id shouldBe "Europe/Amsterdam"
        }
    }
})
