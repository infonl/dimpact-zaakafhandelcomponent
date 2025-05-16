/*
 * SPDX-FileCopyrightText: 2023 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { KeyValue } from "@angular/common";
import { Pipe, PipeTransform } from "@angular/core";

@Pipe({
  name: "sort",
  pure: true,
})
export class SortPipe implements PipeTransform {
  transform(value: KeyValue<string, string>[], property: "key" | "value") {
    return value.sort((a, b) => a[property].localeCompare(b[property]));
  }
}
