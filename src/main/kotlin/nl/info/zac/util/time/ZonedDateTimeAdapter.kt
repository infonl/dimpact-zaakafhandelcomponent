/*
 * SPDX-FileCopyrightText: 2021 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.util.time

import jakarta.json.bind.adapter.JsonbAdapter
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class ZonedDateTimeAdapter : JsonbAdapter<ZonedDateTime, String> {
    override fun adaptToJson(dateTime: ZonedDateTime?): String? = dateTime?.format(DateTimeFormatter.ISO_INSTANT)

    override fun adaptFromJson(dateTime: String): ZonedDateTime = ZonedDateTime.parse(dateTime)
}
