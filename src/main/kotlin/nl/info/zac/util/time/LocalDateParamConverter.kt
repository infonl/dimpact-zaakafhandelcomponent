/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.util.time

import jakarta.ws.rs.BadRequestException
import jakarta.ws.rs.ext.ParamConverter
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Converts a query or path parameter in the ISO date format (`2025-01-31`) to a [LocalDate].
 */
class LocalDateParamConverter : ParamConverter<LocalDate> {
    companion object {
        private val LOG = Logger.getLogger(LocalDateParamConverter::class.java.name)
    }

    override fun fromString(param: String): LocalDate =
        try {
            LocalDate.parse(param, DateTimeFormatter.ISO_LOCAL_DATE)
        } catch (dateTimeParseException: DateTimeParseException) {
            LOG.log(Level.WARNING, dateTimeParseException) { "Could not parse date string: $param" }
            throw BadRequestException(dateTimeParseException)
        }

    override fun toString(date: LocalDate): String = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
}
