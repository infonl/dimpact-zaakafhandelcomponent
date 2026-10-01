/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { provideMomentDateAdapter } from "@angular/material-moment-adapter";
import { MatDateFormats } from "@angular/material/core";

const ZAC_DATE_FORMATS: MatDateFormats = {
  parse: {
    dateInput: "yyyy-MM-DD",
  },
  display: {
    dateInput: "yyyy-MM-DD",
    monthYearLabel: "MMMM YYYY",
    dateA11yLabel: "LL",
    monthYearA11yLabel: "MMMM YYYY",
  },
};

export function provideZacDateAdapter() {
  return provideMomentDateAdapter(ZAC_DATE_FORMATS, { strict: false });
}
