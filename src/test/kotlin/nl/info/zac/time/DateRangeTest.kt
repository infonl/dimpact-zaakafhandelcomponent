/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.time

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.Expression
import jakarta.persistence.criteria.Predicate
import java.time.Instant
import java.time.LocalDate
import java.time.ZonedDateTime

class DateRangeTest : BehaviorSpec({
    extension(UtcDefaultTimeZone())

    val day = LocalDate.of(2026, 7, 30)
    val startOfDay = Instant.parse("2026-07-29T22:00:00Z")
    val startOfNextDay = Instant.parse("2026-07-30T22:00:00Z")

    afterEach {
        checkUnnecessaryStub()
    }

    context("Creating a date range from optional bounds") {
        given("a start date and an end date") {
            then("the range is between the two dates") {
                DateRange.of(from = day, through = day.plusDays(1)) shouldBe DateRange.Between(day, day.plusDays(1))
            }
        }

        given("only a start date") {
            then("the range has no end") {
                DateRange.of(from = day, through = null) shouldBe DateRange.From(day)
            }
        }

        given("only an end date") {
            then("the range has no start") {
                DateRange.of(from = null, through = day) shouldBe DateRange.Through(day)
            }
        }

        given("no start date and no end date") {
            then("there is no range") {
                DateRange.of(from = null, through = null) shouldBe null
            }
        }
    }

    context("Converting a date range to a time range") {
        given("a range from 30-07-2026 through 30-07-2026 and the JVM time zone UTC") {
            val timeRange = DateRange.Between(day, day).toTimeRange()

            then("it runs from the start of 30-07-2026 up to the start of 31-07-2026 in Europe/Amsterdam") {
                timeRange as TimeRange.Between
                timeRange.from.toInstant() shouldBe startOfDay
                timeRange.until.toInstant() shouldBe startOfNextDay
                timeRange.from.zone shouldBe ZacTime.ZONE
                timeRange.until.zone shouldBe ZacTime.ZONE
            }
        }

        given("a range from 01-07-2026 through 30-07-2026") {
            val timeRange = DateRange.Between(LocalDate.of(2026, 7, 1), day).toTimeRange()

            then("it runs from the start of the first day up to the start of the day after the last day") {
                timeRange shouldBe TimeRange.Between(
                    from = ZonedDateTime.of(2026, 7, 1, 0, 0, 0, 0, ZacTime.ZONE),
                    until = ZonedDateTime.of(2026, 7, 31, 0, 0, 0, 0, ZacTime.ZONE)
                )
            }
        }

        given("a range with only a start date") {
            val timeRange = DateRange.From(day).toTimeRange()

            then("it has no end") {
                timeRange shouldBe TimeRange.From(ZacTime.startOfDay(day))
            }
        }

        given("a range with only an end date") {
            val timeRange = DateRange.Through(day).toTimeRange()

            then("it has no start") {
                timeRange shouldBe TimeRange.Until(ZacTime.startOfDay(day.plusDays(1)))
            }
        }

        given("a range through 30-07-2026") {
            val timeRange = DateRange.Through(day).toTimeRange() as TimeRange.Until

            then("the last moment of 30-07-2026 in Europe/Amsterdam is before the end") {
                ZonedDateTime.of(2026, 7, 30, 23, 59, 59, 0, ZacTime.ZONE).isBefore(timeRange.until) shouldBe true
            }
            and("the start of 31-07-2026 in Europe/Amsterdam is not before the end") {
                ZonedDateTime.of(2026, 7, 31, 0, 0, 0, 0, ZacTime.ZONE).isBefore(timeRange.until) shouldBe false
            }
        }

        given("a range from 30-07-2026") {
            val timeRange = DateRange.From(day).toTimeRange() as TimeRange.From

            then("29-07-2026 23:59 in Europe/Amsterdam is before the start") {
                ZonedDateTime.of(2026, 7, 29, 23, 59, 0, 0, ZacTime.ZONE).isBefore(timeRange.from) shouldBe true
            }
        }
    }

    context("Limiting a field with a date and a time to a date range") {
        val builder = mockk<CriteriaBuilder>()
        val field = mockk<Expression<ZonedDateTime>>()
        val fromPredicate = mockk<Predicate>()
        val untilPredicate = mockk<Predicate>()

        given("a range from 30-07-2026 through 30-07-2026") {
            every { builder.greaterThanOrEqualTo(field, ZacTime.startOfDay(day)) } returns fromPredicate
            every { builder.lessThan(field, ZacTime.startOfDay(day.plusDays(1))) } returns untilPredicate

            `when`("the predicates are created") {
                val predicates = DateRange.Between(day, day).toPredicates(builder, field)

                then("the field is at or after the start of the first day and before the start of the next day") {
                    predicates shouldBe listOf(fromPredicate, untilPredicate)
                }
            }
        }

        given("a range with only a start date") {
            every { builder.greaterThanOrEqualTo(field, ZacTime.startOfDay(day)) } returns fromPredicate

            `when`("the predicates are created") {
                val predicates = DateRange.From(day).toPredicates(builder, field)

                then("the field is only limited by the start of the first day") {
                    predicates shouldBe listOf(fromPredicate)
                }
            }
        }
    }

    context("Limiting a field with only a date to a date range") {
        val builder = mockk<CriteriaBuilder>()
        val field = mockk<Expression<LocalDate>>()
        val fromPredicate = mockk<Predicate>()
        val untilPredicate = mockk<Predicate>()

        given("a range from 30-07-2026 through 30-07-2026") {
            every { builder.greaterThanOrEqualTo(field, day) } returns fromPredicate
            every { builder.lessThan(field, day.plusDays(1)) } returns untilPredicate

            `when`("the predicates are created") {
                val predicates = DateRange.Between(day, day).toDatePredicates(builder, field)

                then("the field is on or after the first day and before the day after the last day") {
                    predicates shouldBe listOf(fromPredicate, untilPredicate)
                }
            }
        }

        given("a range with only an end date") {
            every { builder.lessThan(field, day.plusDays(1)) } returns untilPredicate

            `when`("the predicates are created") {
                val predicates = DateRange.Through(day).toDatePredicates(builder, field)

                then("the field is only limited by the day after the last day") {
                    predicates shouldBe listOf(untilPredicate)
                }
            }
        }
    }
})
