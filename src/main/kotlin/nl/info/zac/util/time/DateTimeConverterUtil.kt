/*
 * SPDX-FileCopyrightText: 2021 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.util.time

import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Date

fun Date.toLocalDate(): LocalDate = LocalDate.ofInstant(toInstant(), ZoneId.systemDefault())

fun Date.toZonedDateTime(): ZonedDateTime = toInstant().atZone(ZoneId.systemDefault())

fun LocalDate.toDate(): Date = Date.from(atStartOfDay(ZoneId.systemDefault()).toInstant())
