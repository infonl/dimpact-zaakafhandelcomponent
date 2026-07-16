/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.util.time

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Date

class DateTimeConverterUtilTest : BehaviorSpec({

    given("a date") {
        val date = Date.from(LocalDate.of(2024, 3, 11).atStartOfDay(ZoneId.systemDefault()).toInstant())

        `when`("converting to a LocalDate") {
            val result = DateTimeConverterUtil.convertToLocalDate(date)

            then("it should return the corresponding LocalDate") {
                result shouldBe LocalDate.of(2024, 3, 11)
            }
        }

        `when`("converting to a ZonedDateTime") {
            val result = DateTimeConverterUtil.convertToZonedDateTime(date)

            then("it should return the corresponding ZonedDateTime") {
                result shouldBe ZonedDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault())
            }
        }
    }

    given("a null LocalDate") {
        `when`("converting to a Date") {
            val result = DateTimeConverterUtil.convertToDate(null as LocalDate?)

            then("it should return null") {
                result shouldBe null
            }
        }
    }

    given("a LocalDate") {
        val localDate = LocalDate.of(2024, 3, 11)

        `when`("converting to a Date") {
            val result = DateTimeConverterUtil.convertToDate(localDate)

            then("it should return the corresponding Date at start of day") {
                result shouldBe Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant())
            }
        }
    }

    given("an OffsetDateTime") {
        val offsetDateTime = OffsetDateTime.parse("2024-03-11T10:44:00+01:00")

        `when`("converting to a Date") {
            val result = DateTimeConverterUtil.convertToDate(offsetDateTime)

            then("it should return the corresponding Date") {
                result shouldBe Date.from(
                    offsetDateTime.toZonedDateTime().withZoneSameInstant(ZoneId.systemDefault()).toInstant()
                )
            }
        }
    }

    given("a null ZonedDateTime") {
        `when`("converting to a Date") {
            val result = DateTimeConverterUtil.convertToDate(null as ZonedDateTime?)

            then("it should return null") {
                result shouldBe null
            }
        }
    }

    given("a ZonedDateTime") {
        val zonedDateTime = ZonedDateTime.parse("2024-03-11T10:44:00+01:00")

        `when`("converting to a Date") {
            val result = DateTimeConverterUtil.convertToDate(zonedDateTime)

            then("it should return the corresponding Date") {
                result shouldBe Date.from(
                    zonedDateTime.withZoneSameInstant(ZoneId.systemDefault()).toInstant()
                )
            }
        }

        `when`("converting to a LocalDateTime") {
            val result = DateTimeConverterUtil.convertToLocalDateTime(zonedDateTime)

            then("it should return the corresponding LocalDateTime") {
                result shouldBe zonedDateTime.withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime()
            }
        }
    }

    given("a blank ISO string") {
        `when`("converting to a Date") {
            val result = DateTimeConverterUtil.convertToDate("")

            then("it should return null") {
                result shouldBe null
            }
        }
    }

    given("a well-formed ISO string") {
        val isoString = "2024-03-11T10:44:00+01:00"

        `when`("converting to a Date") {
            val result = DateTimeConverterUtil.convertToDate(isoString)

            then("it should return the corresponding Date") {
                result shouldBe Date.from(
                    ZonedDateTime.parse(isoString).withZoneSameInstant(ZoneId.systemDefault()).toInstant()
                )
            }
        }
    }
})
