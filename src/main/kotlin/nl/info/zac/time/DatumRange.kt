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
 * A date range "van t/m": it includes the whole [tot] day. A `null` bound means no bound.
 * See ADR 0015.
 */
data class DatumRange(val van: LocalDate?, val tot: LocalDate?) {
    /**
     * The first day after the range, the exclusive upper bound of the range.
     */
    fun dayAfterTot(): LocalDate? = tot?.plusDays(1)

    /**
     * The range as a half-open time range in [ZacTime.ZONE]: from the start of the [van] day up to, but not
     * including, the start of the day after the [tot] day.
     */
    fun toTimeRange() = TimeRange(
        from = van?.let(ZacTime::startOfDay),
        until = dayAfterTot()?.let(ZacTime::startOfDay)
    )
}

/**
 * The predicates that limit a field with a date and a time to the range.
 */
fun DatumRange.toPredicates(builder: CriteriaBuilder, field: Expression<ZonedDateTime>): List<Predicate> =
    toTimeRange().let { timeRange ->
        listOfNotNull(
            timeRange.from?.let { builder.greaterThanOrEqualTo(field, it) },
            timeRange.until?.let { builder.lessThan(field, it) }
        )
    }

/**
 * The predicates that limit a field with only a date to the range.
 */
fun DatumRange.toDatePredicates(builder: CriteriaBuilder, field: Expression<LocalDate>): List<Predicate> =
    listOfNotNull(
        van?.let { builder.greaterThanOrEqualTo(field, it) },
        dayAfterTot()?.let { builder.lessThan(field, it) }
    )
