/*
 * SPDX-FileCopyrightText: 2021 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.time

import jakarta.json.bind.adapter.JsonbAdapter
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class LocalDateAdapter : JsonbAdapter<LocalDate, String> {
    override fun adaptToJson(datum: LocalDate): String = datum.format(DateTimeFormatter.ISO_DATE)

    override fun adaptFromJson(datum: String): LocalDate? = when {
        datum.isBlank() -> null
        datum.any { it in "+TZ" } -> ZonedDateTime.parse(datum).withZoneSameInstant(ZacTime.ZONE).toLocalDate()
        else -> LocalDate.parse(datum)
    }
}
