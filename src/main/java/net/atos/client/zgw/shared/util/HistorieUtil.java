/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.client.zgw.shared.util;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.apache.commons.lang3.BooleanUtils;

import nl.info.client.zgw.drc.model.generated.StatusEnum;
import nl.info.client.zgw.drc.model.generated.VertrouwelijkheidaanduidingEnum;


public final class HistorieUtil {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")
            .withZone(ZoneId.systemDefault());

    private static final String TRUE = "Ja";

    private static final String FALSE = "Nee";

    private HistorieUtil() {
    }

    public static String toWaarde(final LocalDate date) {
        return date != null ? DATE_FORMATTER.format(date) : null;
    }

    public static String toWaarde(final ZonedDateTime date) {
        return date != null ? DATE_TIME_FORMATTER.format(date) : null;
    }

    public static String toWaarde(final StatusEnum statusEnum) {
        return statusEnum != null ? statusEnum.toString() : null;
    }

    public static String toWaarde(final VertrouwelijkheidaanduidingEnum vertrouwelijkheidaanduidingEnum) {
        return vertrouwelijkheidaanduidingEnum != null ? vertrouwelijkheidaanduidingEnum.toString() : null;
    }

    public static String toWaarde(final Boolean bool) {
        return bool != null ? BooleanUtils.toString(bool, TRUE, FALSE) : null;
    }
}
