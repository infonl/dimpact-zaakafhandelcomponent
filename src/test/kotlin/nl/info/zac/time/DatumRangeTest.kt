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

class DatumRangeTest : BehaviorSpec({
    extension(UtcDefaultTimeZone())

    val day = LocalDate.of(2026, 7, 30)
    val startOfDay = Instant.parse("2026-07-29T22:00:00Z")
    val startOfNextDay = Instant.parse("2026-07-30T22:00:00Z")

    afterEach {
        checkUnnecessaryStub()
    }

    context("Converting a date range to a time range") {
        given("a range van 30-07-2026 t/m 30-07-2026 and the JVM time zone UTC") {
            val timeRange = DatumRange(van = day, tot = day).toTimeRange()

            then("it runs from the start of 30-07-2026 up to the start of 31-07-2026 in Europe/Amsterdam") {
                timeRange.from?.toInstant() shouldBe startOfDay
                timeRange.until?.toInstant() shouldBe startOfNextDay
                timeRange.from?.zone shouldBe ZacTime.ZONE
                timeRange.until?.zone shouldBe ZacTime.ZONE
            }
        }

        given("a range van 01-07-2026 t/m 30-07-2026") {
            val timeRange = DatumRange(van = LocalDate.of(2026, 7, 1), tot = day).toTimeRange()

            then("it runs from the start of the van day up to the start of the day after the t/m day") {
                timeRange shouldBe TimeRange(
                    from = ZonedDateTime.of(2026, 7, 1, 0, 0, 0, 0, ZacTime.ZONE),
                    until = ZonedDateTime.of(2026, 7, 31, 0, 0, 0, 0, ZacTime.ZONE)
                )
            }
        }

        given("a range with only a van date") {
            val timeRange = DatumRange(van = day, tot = null).toTimeRange()

            then("it has no upper bound") {
                timeRange shouldBe TimeRange(from = ZacTime.startOfDay(day), until = null)
            }
        }

        given("a range with only a t/m date") {
            val timeRange = DatumRange(van = null, tot = day).toTimeRange()

            then("it has no lower bound") {
                timeRange shouldBe TimeRange(from = null, until = ZacTime.startOfDay(day.plusDays(1)))
            }
        }

        given("a range t/m 30-07-2026") {
            val timeRange = DatumRange(van = null, tot = day).toTimeRange()

            then("the last moment of 30-07-2026 in Europe/Amsterdam is before the upper bound") {
                ZonedDateTime.of(2026, 7, 30, 23, 59, 59, 0, ZacTime.ZONE).isBefore(timeRange.until) shouldBe true
            }
            and("the start of 31-07-2026 in Europe/Amsterdam is not before the upper bound") {
                ZonedDateTime.of(2026, 7, 31, 0, 0, 0, 0, ZacTime.ZONE).isBefore(timeRange.until) shouldBe false
            }
        }

        given("a range van 30-07-2026") {
            val timeRange = DatumRange(van = day, tot = null).toTimeRange()

            then("29-07-2026 23:59 in Europe/Amsterdam is before the lower bound") {
                ZonedDateTime.of(2026, 7, 29, 23, 59, 0, 0, ZacTime.ZONE).isBefore(timeRange.from) shouldBe true
            }
        }

        given("a range without dates") {
            val timeRange = DatumRange(van = null, tot = null).toTimeRange()

            then("it has no bounds") {
                timeRange shouldBe TimeRange(from = null, until = null)
            }
        }
    }

    context("Limiting a field with a date and a time to a date range") {
        val builder = mockk<CriteriaBuilder>()
        val field = mockk<Expression<ZonedDateTime>>()
        val fromPredicate = mockk<Predicate>()
        val untilPredicate = mockk<Predicate>()

        given("a range van 30-07-2026 t/m 30-07-2026") {
            every { builder.greaterThanOrEqualTo(field, ZacTime.startOfDay(day)) } returns fromPredicate
            every { builder.lessThan(field, ZacTime.startOfDay(day.plusDays(1))) } returns untilPredicate

            `when`("the predicates are created") {
                val predicates = DatumRange(van = day, tot = day).toPredicates(builder, field)

                then("the field is at or after the start of the van day and before the start of the next day") {
                    predicates shouldBe listOf(fromPredicate, untilPredicate)
                }
            }
        }

        given("a range without dates") {
            `when`("the predicates are created") {
                val predicates = DatumRange(van = null, tot = null).toPredicates(builder, field)

                then("there are no predicates") {
                    predicates shouldBe emptyList()
                }
            }
        }
    }

    context("Limiting a field with only a date to a date range") {
        val builder = mockk<CriteriaBuilder>()
        val field = mockk<Expression<LocalDate>>()
        val fromPredicate = mockk<Predicate>()
        val untilPredicate = mockk<Predicate>()

        given("a range van 30-07-2026 t/m 30-07-2026") {
            every { builder.greaterThanOrEqualTo(field, day) } returns fromPredicate
            every { builder.lessThan(field, day.plusDays(1)) } returns untilPredicate

            `when`("the predicates are created") {
                val predicates = DatumRange(van = day, tot = day).toDatePredicates(builder, field)

                then("the field is on or after the van day and before the day after the t/m day") {
                    predicates shouldBe listOf(fromPredicate, untilPredicate)
                }
            }
        }

        given("a range with only a t/m date") {
            every { builder.lessThan(field, day.plusDays(1)) } returns untilPredicate

            `when`("the predicates are created") {
                val predicates = DatumRange(van = null, tot = day).toDatePredicates(builder, field)

                then("the field is only limited by the day after the t/m day") {
                    predicates shouldBe listOf(untilPredicate)
                }
            }
        }
    }
})
