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

    given("the JVM time zone UTC and the dates around the change to summer time on 29-03-2026") {
        `when`("the start of each day is determined") {
            val startOfChangeDay = ZacTime.startOfDay(LocalDate.of(2026, 3, 29))
            val startOfNextDay = ZacTime.startOfDay(LocalDate.of(2026, 3, 30))

            then("29-03-2026 starts at 00:00 winter time and 30-03-2026 at 00:00 summer time") {
                startOfChangeDay.toInstant() shouldBe Instant.parse("2026-03-28T23:00:00Z")
                startOfNextDay.toInstant() shouldBe Instant.parse("2026-03-29T22:00:00Z")
            }
        }
    }

    given("the JVM time zone UTC and the dates around the change to winter time on 25-10-2026") {
        `when`("the start of each day is determined") {
            val startOfChangeDay = ZacTime.startOfDay(LocalDate.of(2026, 10, 25))
            val startOfNextDay = ZacTime.startOfDay(LocalDate.of(2026, 10, 26))

            then("25-10-2026 starts at 00:00 summer time and 26-10-2026 at 00:00 winter time") {
                startOfChangeDay.toInstant() shouldBe Instant.parse("2026-10-24T22:00:00Z")
                startOfNextDay.toInstant() shouldBe Instant.parse("2026-10-25T23:00:00Z")
            }
        }
    }

    given("the JVM time zone UTC and a clock in UTC at 23:30 UTC on 25-10-2026, after the change to winter time") {
        val clock = Clock.fixed(Instant.parse("2026-10-25T23:30:00Z"), ZoneOffset.UTC)

        `when`("today is determined") {
            val today = ZacTime.today(clock)

            then("it is 26-10-2026, because Europe/Amsterdam is one hour ahead of UTC in winter time") {
                today shouldBe LocalDate.of(2026, 10, 26)
            }
        }
    }

    given("the JVM time zone UTC and a clock in UTC at 22:30 UTC on 25-10-2026, after the change to winter time") {
        val clock = Clock.fixed(Instant.parse("2026-10-25T22:30:00Z"), ZoneOffset.UTC)

        `when`("today is determined") {
            val today = ZacTime.today(clock)

            then("it is still 25-10-2026, because it is 23:30 in Europe/Amsterdam") {
                today shouldBe LocalDate.of(2026, 10, 25)
            }
        }
    }

    given("the time zone of ZAC") {
        then("it is Europe/Amsterdam") {
            ZacTime.ZONE.id shouldBe "Europe/Amsterdam"
        }
    }
})
