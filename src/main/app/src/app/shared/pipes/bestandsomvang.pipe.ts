/*
 * SPDX-FileCopyrightText: 2023 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { LOCALE_ID, Pipe, PipeTransform, inject } from "@angular/core";
import { KB_IN_BYTES, MB_IN_BYTES } from "../utils/constants";

@Pipe({
  name: "bestandsomvang",
  standalone: true,
})
export class BestandsomvangPipe implements PipeTransform {
  locale = inject(LOCALE_ID);

  transform(value: number) {
    if (value) {
      const stringValue =
        value / MB_IN_BYTES < 1
          ? Math.round(value / KB_IN_BYTES) + " kB"
          : (value / MB_IN_BYTES).toFixed(2) + " MB";
      return stringValue;
    }
    return value;
  }
}
