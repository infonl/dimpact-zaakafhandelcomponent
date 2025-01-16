/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.util.time;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import jakarta.json.bind.adapter.JsonbAdapter;

public class ZonedDateTimeAdapter implements JsonbAdapter<ZonedDateTime, String> {

    @Override
    public String adaptToJson(final ZonedDateTime dateTime) {
        return dateTime != null ? dateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME) : null;
    }

    @Override
    public ZonedDateTime adaptFromJson(final String dateTime) {
        return ZonedDateTime.parse(dateTime, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }
}
