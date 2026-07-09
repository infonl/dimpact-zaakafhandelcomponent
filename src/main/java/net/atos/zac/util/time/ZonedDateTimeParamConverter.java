/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.util.time;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ext.ParamConverter;

/**
 * RestEasy converter ZonedDateTime <-> String
 */
public class ZonedDateTimeParamConverter implements ParamConverter<ZonedDateTime> {

    private static final Logger LOG = Logger.getLogger(ZonedDateTimeParamConverter.class.getName());

    @Override
    public ZonedDateTime fromString(String param) {
        try {
            return ZonedDateTime.parse(param, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        } catch (DateTimeParseException e) {
            LOG.log(Level.WARNING, "Could not parse date string: " + param, e);
            throw new BadRequestException(e);
        }
    }

    @Override
    public String toString(ZonedDateTime date) {
        return date.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }
}
