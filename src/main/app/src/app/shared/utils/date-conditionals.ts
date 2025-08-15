/*
 * SPDX-FileCopyrightText: 2021 - 2022 Atos, 2025 INFO
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { FormControl } from "@angular/forms";
import moment, { Moment } from "moment";

export declare type ConditionalFn = (control: FormControl) => boolean;

type Funcs = (typeof DateConditionals)["isExceeded"];

type ShiftTuple<T extends Array<unknown>> = T extends [T[0], ...infer R]
  ? R
  : never;

const DATE_FORMATS = ["YYYY-MM-DD", "DD-MM-YYYY", "MM/DD/YYYY"];

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
    actual?: Date | Moment | string | null,
  ): boolean {
    if (!value) return false;

    const limit = moment(value, DATE_FORMATS, false);
    const compareDate = actual ? moment(actual, DATE_FORMATS, false) : moment();

    return limit.isBefore(compareDate, "day");
  }

  static isPreceded(
    value: Date | Moment | string,
    actual?: Date | Moment | string | null,
  ): boolean {
    if (!value) return false;

    const limit = moment(value, DATE_FORMATS, false);
    const compareDate = actual ? moment(actual, DATE_FORMATS, false) : moment();

    return limit.isAfter(compareDate, "day");
  }

  static always(_value: Date | moment.Moment | string) {
    console.debug(`Returning true for`, { _value });
    return true;
  }
}
