/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023-2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.util;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import jakarta.json.bind.adapter.JsonbAdapter;

public class ZonedDateTimeAdapter implements JsonbAdapter<ZonedDateTime, String> {

    @Override
    public String adaptToJson(final ZonedDateTime dateTime) {
        return dateTime != null ? dateTime.format(DateTimeFormatter.ISO_INSTANT) : null;
    }

    @Override
    public ZonedDateTime adaptFromJson(final String dateTime) {
        return ZonedDateTime.parse(dateTime);
    }
}
