/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.itest.util

import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime

/**
 * The time zone in which ZAC computes and reads dates, whatever the time zone of its server.
 * The integration tests cannot use `ZacTime` of the main code, so they repeat it here. See ADR 0015.
 */
val ZAC_ZONE: ZoneId = ZoneId.of("Europe/Amsterdam")

/**
 * Today for a date that ZAC computes or reads.
 */
fun todayInZac(): LocalDate = LocalDate.now(ZAC_ZONE)

fun nowInZac(): ZonedDateTime = ZonedDateTime.now(ZAC_ZONE)

/**
 * Today in UTC, the time zone of Open Zaak: for a date that Open Zaak computes or validates, that a test sends to
 * Open Zaak directly, or that ZAC returns as part of an instant in UTC.
 */
fun todayInUtc(): LocalDate = LocalDate.now(ZoneOffset.UTC)
