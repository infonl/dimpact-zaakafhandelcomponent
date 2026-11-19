/*
 * SPDX-FileCopyrightText: 2021 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.time

import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.Date

fun Date.toLocalDate(): LocalDate = LocalDate.ofInstant(toInstant(), ZacTime.ZONE)

fun Date.toZonedDateTime(): ZonedDateTime = toInstant().atZone(ZacTime.ZONE)

fun LocalDate.toDate(): Date = Date.from(ZacTime.startOfDay(this).toInstant())
