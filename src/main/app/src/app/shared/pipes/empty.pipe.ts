/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Pipe, PipeTransform } from "@angular/core";

@Pipe({
  name: "empty",
})
export class EmptyPipe implements PipeTransform {
  private readonly EMPTY_STRING = "-";

  transform(value: unknown, property?: string) {
    if(!value) return this.EMPTY_STRING;

    if (property && value instanceof Object) {
      if(property in value) {
        const objectValue = value[property as keyof typeof value]
        if(!objectValue) return this.EMPTY_STRING;
        return objectValue.length ? String(objectValue) : this.EMPTY_STRING;
      }
      return this.EMPTY_STRING
    }

    if(!String(value)) return this.EMPTY_STRING;

    return String(value)
  }
}
