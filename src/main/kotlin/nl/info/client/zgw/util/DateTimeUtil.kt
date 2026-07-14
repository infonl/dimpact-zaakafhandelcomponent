/*
 * SPDX-FileCopyrightText: 2021 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.util

import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

const val DATE_TIME_FORMAT_WITH_MILLISECONDS = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSX"

fun LocalDate.convertToDateTime(): ZonedDateTime = atStartOfDay(ZoneId.systemDefault())
