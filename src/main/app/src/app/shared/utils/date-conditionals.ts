/*
 * SPDX-FileCopyrightText: 2021 - 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { FormControl } from "@angular/forms";
import { DateInput, isEarlierDay, isLaterDay, now } from "../../date-time";

export declare type ConditionalFn = (control: FormControl) => boolean;

type Funcs = (typeof DateConditionals)["isExceeded"];

type ShiftTuple<T extends Array<unknown>> = T extends [T[0], ...infer R]
  ? R
  : never;

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
    value: DateInput | null,
    actual?: DateInput | null,
  ): boolean {
    if (!value) return false;

    return isEarlierDay(value, actual || now());
  }

  static isPreceded(value: DateInput, actual?: DateInput | null): boolean {
    if (!value) return false;

    return isLaterDay(value, actual || now());
  }

  static always(_value: DateInput) {
    console.warn("condition always returns true", _value);
    return true;
  }
}
