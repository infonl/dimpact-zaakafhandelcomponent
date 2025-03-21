/*
 * SPDX-FileCopyrightText: <YYYY> Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 *
 */

import { AbstractControl, ValidatorFn } from "@angular/forms";
import { TranslateService } from "@ngx-translate/core";
import moment from "moment";

export class CustomValidators {
  static minDate(minDate: Date | string | moment.Moment) {
    return (control: AbstractControl) => {
      if (!control.value) {
        return null;
      }

      const date = moment(minDate);
      const value = moment(control.value);

      return value.isBefore(date)
        ? { [CustomValidators.minDate.name]: true }
        : null;
    };
  }

  /**
   * Validator that set a max date on a date field. This validator only works together with an `zac-date` component.
   *
   * # Usage notes
   * ## Validate that the field has a maximum date
   * ```js
   * const control = new FormControl(moment(), CustomValidators.maxDate(moment().subtract(1, 'day')));
   *
   * console.log(control.errors); // { maxDate: { maxDate: 'DD/MM/YYYY' , actualDate: 'DD/MM/YYYY' } }
   * ```
   * @returns: A validator function that returns an error map with the `maxDate` property if the validation check fails, otherwise `null`.
   */
  static maxDate(maxDate: Date | string | moment.Moment): ValidatorFn {
    return (control: AbstractControl) => {
      if (!control.value) return null;

      const date = moment(maxDate);
      const value = moment(control.value);

      if (!value.isAfter(date)) return null;

      return {
        [CustomValidators.maxDate.name]: {
          [CustomValidators.maxDate.name]: date.format(),
          actualDate: value.format(),
        },
      };
    };
  }

  static getErrorMessage(
    control?: AbstractControl,
    translateService?: TranslateService,
  ) {
    if (!control?.errors) return null;

    const [error, parameters] = Object.entries(control.errors)[0];

    console.log({ error, parameters });

    return translateService?.instant(`validators.${error}`, parameters);
  }
}
