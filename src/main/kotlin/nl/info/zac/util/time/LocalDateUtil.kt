/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.util.time

import nl.info.client.zgw.ztc.model.generated.BesluitType
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")

/**
 * Returns whether [LocalDate].now() is between two dates.
 *
 * @param begin The lower-end of the date range
 * @param end   The higher-end of the date range
 * @return true if now <= begin and now < end, false otherwise. If any end of the date range is null it is not compared.
 */
fun dateNowIsBetween(begin: LocalDate?, end: LocalDate?): Boolean {
    val now = LocalDate.now()
    return (begin == null || begin.isBefore(now) || begin.isEqual(now)) && (end == null || end.isAfter(now))
}

fun dateNowIsBetween(besluitType: BesluitType): Boolean =
    dateNowIsBetween(begin = besluitType.beginGeldigheid, end = besluitType.eindeGeldigheid)

fun format(date: String?): String? = date?.let { LocalDate.parse(it).format(DATE_FORMATTER) }
