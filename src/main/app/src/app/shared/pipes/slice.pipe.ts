/*
 * SPDX-FileCopyrightText: 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Pipe, PipeTransform } from "@angular/core";

@Pipe({ name: "slice" })
export class SlicePipe implements PipeTransform {
  transform(value: string, start: number, end?: number): string {
    if (typeof value !== "string") {
      return value;
    }
    return value.slice(start, end);
  }
}
