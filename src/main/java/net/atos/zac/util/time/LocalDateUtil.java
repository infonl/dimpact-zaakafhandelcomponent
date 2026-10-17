/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.util.time;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import nl.info.client.zgw.ztc.model.generated.BesluitType;

public final class LocalDateUtil {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private LocalDateUtil() {
    }

    /**
     * Returns whether {@link LocalDate}.now() is between two dates.
     *
     * @param begin The lower-end of the date range
     * @param end   The higher-end of the date range
     * @return true if now <= begin and now < end, false otherwise. If any end of the date range is null it is not compared.
     */
    public static boolean dateNowIsBetween(LocalDate begin, LocalDate end) {
        final LocalDate now = LocalDate.now();
        return (begin == null || begin.isBefore(now) || begin.isEqual(now)) && (end == null || end.isAfter(now));
    }

    public static boolean dateNowIsBetween(BesluitType besluittype) {
        return dateNowIsBetween(besluittype.getBeginGeldigheid(), besluittype.getEindeGeldigheid());
    }

    public static String format(String date) {
        return LocalDate.parse(date).format(DATE_FORMATTER);
    }
}
