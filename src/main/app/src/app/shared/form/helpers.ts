/*
 * SPDX-FileCopyrightText: <YYYY> Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 *
 */

import { AbstractControl, ValidationErrors, Validators } from "@angular/forms";
import moment from "moment";

export function getErrorMessage(control?: AbstractControl) {
  if (!control?.errors) {
    return "an unknown error occurred";
  }

  const commonErrors = [
    Validators.required.name,
    Validators.min.name,
    Validators.max.name,
    Validators.email.name,
    Validators.pattern.name,
    Validators.maxLength.name,
    Validators.minLength.name,
  ];

  for (const error of commonErrors) {
    if (control.hasError(error)) {
      return commonErrorMessage(error);
    }
  }

  return customErrorMessage(control.errors);
}

function commonErrorMessage(error: string) {
  switch (error) {
    case Validators.required.name:
      return "This field is required";
    case Validators.min.name:
      return "This value is too low";
    case Validators.max.name:
      return "This value is too high";
    case Validators.maxLength.name:
      return "This value is too long";
    case Validators.minLength.name:
      return "This value is too short";
    case Validators.email.name:
      return "This is not a valid email address";
    case Validators.pattern.name:
      return "This value is invalid";
    default:
      console.log({ error });
      return "unknown common validation error";
  }
}

function customErrorMessage(errors: ValidationErrors) {
  const errorKeys = Object.keys(errors);

  if (errorKeys.includes("minDate")) {
    return "This date is too far back";
  } else if (errorKeys.includes("maxDate")) {
    return "This date is too far in the future";
  }

  return "unknown custom validation error";
}

export class CustomValidators {
  static minDate(minDate: Date | string | moment.Moment) {
    return (control: AbstractControl) => {
      if (!control.value) {
        return null;
      }

      const date = moment(minDate);
      const value = moment(control.value);

      return value.isBefore(date) ? { minDate: true } : null;
    };
  }

  static maxDate(maxDate: Date | string | moment.Moment) {
    return (control: AbstractControl) => {
      if (!control.value) {
        return null;
      }

      const date = moment(maxDate);
      const value = moment(control.value);

      return value.isAfter(date) ? { maxDate: true } : null;
    };
  }
}
