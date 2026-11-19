/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.time

import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * The time zone of ZAC and the current date and time in it, independent of the time zone of the server.
 * See ADR 0015.
 */
object ZacTime {
    val ZONE: ZoneId = ZoneId.of("Europe/Amsterdam")

    fun today(clock: Clock = Clock.system(ZONE)): LocalDate = LocalDate.now(clock.withZone(ZONE))

    fun now(clock: Clock = Clock.system(ZONE)): ZonedDateTime = ZonedDateTime.now(clock.withZone(ZONE))

    fun startOfDay(date: LocalDate): ZonedDateTime = date.atStartOfDay(ZONE)
}
