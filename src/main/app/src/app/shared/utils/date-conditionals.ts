/*
 * SPDX-FileCopyrightText: 2021 - 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { FormControl } from "@angular/forms";
// @ts-ignore-next-line allow to import moment
import moment, { Moment } from "moment";

export declare type ConditionalFn = (control: FormControl) => boolean;

type Funcs = (typeof DateConditionals)["isExceeded"];

type ShiftTuple<T extends any[]> = T extends [T[0], ...infer R] ? R : never;

export class DateConditionals {
  static provideFormControlValue<
    A extends Funcs,
    B extends ShiftTuple<Parameters<A>>,
  >(method: A, ...attributes: Exclude<B, "value">): ConditionalFn {
    return (control: FormControl): boolean => {
      return method(control.value, ...attributes);
    };
  }

  static isExceeded(
    value: Date | Moment | string,
    actual?: Date | Moment | string,
  ): boolean {
    if (value) {
      const limit: Moment = moment(value);
      if (actual) {
        const actualDate = moment(actual);
        return limit.isBefore(actualDate, "day");
      } else {
        const currentDate = moment();
        return limit.isBefore(currentDate, "day");
      }
    }
    return false;
  }

  static always(_value: Date | moment.Moment | string) {
    console.debug(`Returning true for`, { _value });
    return true;
  }
}
