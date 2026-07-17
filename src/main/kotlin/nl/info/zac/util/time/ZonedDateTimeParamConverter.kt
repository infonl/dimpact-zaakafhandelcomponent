/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.util.time

import jakarta.ws.rs.BadRequestException
import jakarta.ws.rs.ext.ParamConverter
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.logging.Level
import java.util.logging.Logger

/**
 * RestEasy converter ZonedDateTime <-> String
 */
class ZonedDateTimeParamConverter : ParamConverter<ZonedDateTime> {
    companion object {
        private val LOG = Logger.getLogger(ZonedDateTimeParamConverter::class.java.name)
    }

    override fun fromString(param: String): ZonedDateTime =
        try {
            ZonedDateTime.parse(param, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        } catch (dateTimeParseException: DateTimeParseException) {
            LOG.log(Level.WARNING, dateTimeParseException) { "Could not parse date string: $param" }
            throw BadRequestException(dateTimeParseException)
        }

    override fun toString(date: ZonedDateTime): String = date.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
}
