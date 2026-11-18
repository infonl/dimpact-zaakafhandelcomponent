/*
 * SPDX-FileCopyrightText: 2021 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { LOCALE_ID, Pipe, PipeTransform, inject } from "@angular/core";
import { DateInput, DisplayFormat, formatForDisplay } from "../../date-time";

@Pipe({
  name: "datum",
  standalone: true,
})
export class DatumPipe implements PipeTransform {
  readonly locale = inject(LOCALE_ID);

  transform(value?: DateInput | null, dateFormat?: DisplayFormat) {
    if (!value) return value;

    const formatted = formatForDisplay(
      value,
      dateFormat ?? "shortDate",
      this.locale,
    );

    if (formatted === null) return value.toString();

    // Format dates with hard non-breaking hyphens, because the normal soft hyphens in a date will be seen
    // by the browser as a point where a new line can be started if necessary. Replacing soft hyphens with
    // hard hyphens prevents that meaning that the date will either remain on the same line or moved as a
    // whole to the next line.
    return formatted.replace(/-/g, "\u2011");
  }
}
