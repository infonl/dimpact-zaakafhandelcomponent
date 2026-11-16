/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.time

import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.Expression
import jakarta.persistence.criteria.Predicate
import java.time.LocalDate
import java.time.ZonedDateTime

/**
 * A date range that includes the whole end date. The type says which bounds the range has. See ADR 0015.
 */
sealed interface DateRange {
    data class Between(val from: LocalDate, val through: LocalDate) : DateRange

    data class From(val from: LocalDate) : DateRange

    data class Through(val through: LocalDate) : DateRange

    companion object {
        /**
         * The range with the given bounds, or `null` when neither bound is given.
         */
        fun of(from: LocalDate?, through: LocalDate?): DateRange? = when {
            from != null && through != null -> Between(from, through)
            from != null -> From(from)
            through != null -> Through(through)
            else -> null
        }
    }
}

/**
 * The range as a half-open time range in [ZacTime.ZONE]: from the start of the first day up to, but not including,
 * the start of the day after the last day.
 */
fun DateRange.toTimeRange() = when (this) {
    is DateRange.Between -> TimeRange.Between(ZacTime.startOfDay(from), startOfDayAfter(through))
    is DateRange.From -> TimeRange.From(ZacTime.startOfDay(from))
    is DateRange.Through -> TimeRange.Until(startOfDayAfter(through))
}

/**
 * The predicates that limit a field with a date and a time to the range.
 */
fun DateRange.toPredicates(builder: CriteriaBuilder, field: Expression<ZonedDateTime>) =
    when (val timeRange = toTimeRange()) {
        is TimeRange.Between -> listOf(
            builder.greaterThanOrEqualTo(field, timeRange.from),
            builder.lessThan(field, timeRange.until)
        )
        is TimeRange.From -> listOf(builder.greaterThanOrEqualTo(field, timeRange.from))
        is TimeRange.Until -> listOf(builder.lessThan(field, timeRange.until))
    }

/**
 * The predicates that limit a field with only a date to the range.
 */
fun DateRange.toDatePredicates(builder: CriteriaBuilder, field: Expression<LocalDate>) = when (this) {
    is DateRange.Between -> listOf(
        builder.greaterThanOrEqualTo(field, from),
        builder.lessThan(field, through.plusDays(1))
    )
    is DateRange.From -> listOf(builder.greaterThanOrEqualTo(field, from))
    is DateRange.Through -> listOf(builder.lessThan(field, through.plusDays(1)))
}

private fun startOfDayAfter(date: LocalDate) = ZacTime.startOfDay(date.plusDays(1))
