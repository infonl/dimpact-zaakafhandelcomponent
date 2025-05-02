/*
 * SPDX-FileCopyrightText: 2023 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Inject, LOCALE_ID, Pipe, PipeTransform } from "@angular/core";
import { KB_IN_BYTES, MB_IN_BYTES } from "../utils/constants";

@Pipe({ name: "bestandsomvang" })
export class BestandsomvangPipe implements PipeTransform {
  constructor(@Inject(LOCALE_ID) public locale: string) {}

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
