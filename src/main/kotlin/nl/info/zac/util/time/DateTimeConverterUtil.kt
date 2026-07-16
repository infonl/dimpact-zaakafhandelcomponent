/*
 * SPDX-FileCopyrightText: 2021 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.util.time

import org.apache.commons.lang3.StringUtils
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Date

object DateTimeConverterUtil {
    private val DEFAULT_ZONE_ID: ZoneId = ZoneId.systemDefault()

    fun convertToLocalDate(date: Date?): LocalDate? = date?.let { LocalDate.ofInstant(it.toInstant(), DEFAULT_ZONE_ID) }

    fun convertToZonedDateTime(dateTime: Date?): ZonedDateTime? =
        dateTime?.let { ZonedDateTime.ofInstant(it.toInstant(), DEFAULT_ZONE_ID) }

    fun convertToDate(localDate: LocalDate?): Date? =
        localDate?.let { Date.from(it.atStartOfDay().atZone(DEFAULT_ZONE_ID).toInstant()) }

    fun convertToDate(offsetDateTime: OffsetDateTime?): Date? =
        offsetDateTime?.let { Date.from(it.toZonedDateTime().withZoneSameInstant(DEFAULT_ZONE_ID).toInstant()) }

    fun convertToDate(zonedDateTime: ZonedDateTime?): Date? =
        zonedDateTime?.let { Date.from(it.withZoneSameInstant(DEFAULT_ZONE_ID).toInstant()) }

    fun convertToDate(isoString: String?): Date? =
        if (StringUtils.isNotBlank(isoString)) convertToDate(ZonedDateTime.parse(isoString)) else null

    fun convertToLocalDateTime(zonedDateTime: ZonedDateTime?): LocalDateTime? =
        zonedDateTime?.let { it.withZoneSameInstant(DEFAULT_ZONE_ID).toLocalDateTime() }
}
