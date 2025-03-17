/*
 * SPDX-FileCopyrightText: <YYYY> Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 *
 */

import {AbstractControl, ValidationErrors, Validators} from "@angular/forms";
import moment from "moment";

export function getErrorMessage(control?: AbstractControl) {
  if (!control) {
    return "unknown error";
  }

  const commonErrors = [
    Validators.required.name,
    Validators.min.name,
    Validators.max.name,
    Validators.email.name,
    Validators.pattern.name,
  ];

  for (const error of commonErrors) {
    if (control.hasError(error)) {
      return errorMessage(error);
    }
  }

  return "unknown error";
}

function errorMessage(error: string) {
  switch (error) {
    case Validators.required.name:
      return "This field is required";
    case Validators.min.name:
      return "This value is too low";
    case Validators.max.name:
      return "This value is too high";
    case Validators.email.name:
      return "This is not a valid email address";
    case Validators.pattern.name:
      return "This value is invalid";
    default:
        console.log({error})
      return "unknown error";
  }
}

export class CustomValidators {
    static minDate(minDate: Date | string | moment.Moment) {
        return (control: AbstractControl) => {
            if (!control.value) {
                return null;
            }

            const date = moment(minDate)
            const value = moment(control.value)

            return value.isBefore(date) ? { minDate: true } : null;
        };
    }
}
