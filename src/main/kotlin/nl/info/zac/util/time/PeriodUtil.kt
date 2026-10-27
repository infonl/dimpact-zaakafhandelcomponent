/*
 * SPDX-FileCopyrightText: 2021 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.util.time

import java.time.LocalDateTime
import java.time.Period
import java.time.temporal.ChronoUnit

fun Period.format(): String = when (this) {
    Period.ZERO -> "0 dagen"
    else -> listOfNotNull(
        formatUnit(amount = years, singular = "jaar", plural = "jaren"),
        formatUnit(amount = months, singular = "maand", plural = "maanden"),
        formatUnit(amount = days, singular = "dag", plural = "dagen")
    ).joinToString(", ")
}

fun Period.numberOfDaysFromToday(): Int {
    val start = LocalDateTime.now()
    return start.until(start.plus(this), ChronoUnit.DAYS).toInt()
}

private fun formatUnit(amount: Int, singular: String, plural: String): String? = when (amount) {
    0 -> null
    -1, 1 -> "$amount $singular"
    else -> "$amount $plural"
}
