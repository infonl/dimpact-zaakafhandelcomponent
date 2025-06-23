/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 *
 */

import { AbstractControl, FormControl, Validators } from "@angular/forms";
import { TranslateService } from "@ngx-translate/core";

export class FormHelper {
  static getErrorMessage(
    control?: AbstractControl,
    translateService?: TranslateService,
  ) {
    if (!control?.errors) return null;

    const [error, parameters] = Object.entries(control.errors)[0];

    if (error === "custom") {
      const { message, ...rest } = parameters as ReturnType<
        typeof FormHelper.CustomErrorMessage
      >["custom"];
      return translateService?.instant(String(message), rest);
    }

    return translateService?.instant(`validators.${error}`, parameters);
  }

  static getValidatorValue(
    key: keyof typeof Validators,
    control?: AbstractControl,
  ) {
    if (!control?.validator) return null;

    switch (key) {
      case Validators.maxLength.name:
        return control.validator(new FormControl({ length: Infinity }))
          ?.maxlength?.requiredLength as number | null;
      case Validators.minLength.name:
        return control.validator(new FormControl({ length: -Infinity }))
          ?.minLength?.requiredLength as number | null;
      case Validators.min.name:
        return control.validator(new FormControl(-Infinity))?.min?.min as
          | number
          | null;
      case Validators.max.name:
        return control.validator(new FormControl(Infinity))?.max?.max as
          | number
          | null;
      default:
        return null;
    }
  }

  static CustomErrorMessage(key: string, params?: Record<string, unknown>) {
    return {
      custom: {
        message: key,
        ...params,
      },
    };
  }
}
