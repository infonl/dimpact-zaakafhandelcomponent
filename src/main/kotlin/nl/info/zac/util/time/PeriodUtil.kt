/*
 * SPDX-FileCopyrightText: 2021 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.util.time

import java.time.LocalDateTime
import java.time.Period
import java.time.temporal.ChronoUnit

object PeriodUtil {
    fun format(period: Period?): String? = when (period) {
        null -> null
        Period.ZERO -> "0 dagen"
        else -> listOfNotNull(
            formatUnit(amount = period.years, singular = "jaar", plural = "jaren"),
            formatUnit(amount = period.months, singular = "maand", plural = "maanden"),
            formatUnit(amount = period.days, singular = "dag", plural = "dagen")
        ).joinToString(", ")
    }

    fun numberOfDaysFromToday(period: Period?): Int {
        if (period == null) {
            return 0
        }
        val start = LocalDateTime.now()
        return start.until(start.plus(period), ChronoUnit.DAYS).toInt()
    }

    private fun formatUnit(amount: Int, singular: String, plural: String): String? = when (amount) {
        0 -> null
        -1, 1 -> "$amount $singular"
        else -> "$amount $plural"
    }
}
