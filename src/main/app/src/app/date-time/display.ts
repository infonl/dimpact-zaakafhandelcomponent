/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import moment from "moment";
import "moment/locale/nl";
import { DateInput } from "./picked-date";

export type DisplayFormat =
  | "shortDate"
  | "mediumDate"
  | "longDate"
  | "short"
  | "medium"
  | "long"
  | "full"
  | "fullDate";

const DISPLAY_FORMATS: Record<DisplayFormat, string> = {
  shortDate: "L",
  mediumDate: "ll",
  longDate: "LL",
  short: "L LT",
  medium: "ll LT",
  long: "LL LT",
  full: "LLLL",
  fullDate: "dddd, LL",
};

export function formatForDisplay(
  value: DateInput,
  format: DisplayFormat,
  locale: string,
) {
  const date = moment(value, moment.ISO_8601).locale(locale);
  if (!date.isValid()) return null;
  return date.format(DISPLAY_FORMATS[format] ?? DISPLAY_FORMATS.shortDate);
}
