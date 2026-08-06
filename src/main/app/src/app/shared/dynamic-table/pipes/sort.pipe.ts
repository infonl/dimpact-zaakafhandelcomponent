/*
 * SPDX-FileCopyrightText: 2023 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { KeyValue } from "@angular/common";
import { Pipe, PipeTransform } from "@angular/core";

@Pipe({
  name: "sort",
  pure: true,
  standalone: true,
})
export class SortPipe implements PipeTransform {
  transform<K extends string, V extends string>(
    value: KeyValue<K, V>[],
    property: "key" | "value",
  ) {
    return value.sort((a, b) => a[property].localeCompare(b[property]));
  }
}
