/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Inject, LOCALE_ID, Pipe, PipeTransform } from "@angular/core";
import moment from "moment";

type DateFormat =
  | "shortDate"
  | "mediumDate"
  | "longDate"
  | "short"
  | "medium"
  | "long"
  | "full"
  | "fullDate";

@Pipe({ name: "datum" })
export class DatumPipe implements PipeTransform {
  constructor(@Inject(LOCALE_ID) public locale: string) {}

  transform(
    value?: Date | moment.Moment | string | null,
    dateFormat?: DateFormat,
  ) {
    if (!value) return value;

    const localeDate = moment(value, moment.ISO_8601).locale(this.locale);

    if (!localeDate.isValid()) return value.toString();

    const format = this.getFormat(dateFormat ?? "shortDate");

    // Format dates with hard non-breaking hyphens, because the normal soft hyphens in a date will be seen
    // by the browser as a point where a new line can be started if necessary. Replacing soft hyphens with
    // hard hyphens prevents that meaning that the date will either remain on the same line or moved as a
    // whole to the next line.
    return localeDate.format(format).replace(/-/g, "\u2011");
  }

  // mapping angular format to moment format
  getFormat(dateFormat: DateFormat) {
    switch (dateFormat) {
      case "shortDate":
        return "L";
      case "mediumDate":
        return "ll";
      case "longDate":
        return "LL";
      case "short":
        return "L LT";
      case "medium":
        return "ll LT";
      case "long":
        return "LL LT";
      case "full":
        return "LLLL";
      case "fullDate":
        return "dddd, LL";
      default:
        return "L";
    }
  }
}
