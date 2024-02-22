/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023-2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.zgw.shared.util;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Date-time utility functions and constants.
 */
public final class DateTimeUtil {

  public static final String DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ssX";

  public static final String DATE_TIME_FORMAT_WITH_MILLISECONDS = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSX";

  public static ZonedDateTime convertToDateTime(final LocalDate date) {
    return date.atStartOfDay(ZoneId.systemDefault());
  }

  private DateTimeUtil() {}
}
