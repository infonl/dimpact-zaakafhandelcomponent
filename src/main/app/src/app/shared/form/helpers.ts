/*
 * SPDX-FileCopyrightText: <YYYY> Lifely
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

    return translateService?.instant(`validators.${error}`, parameters);
  }

  static getValidatorValue(
    key: keyof typeof Validators,
    control?: AbstractControl,
  ) {
    switch (key) {
      case Validators.maxLength.name:
        return control?.validator?.(new FormControl({ length: Infinity }))
          ?.maxlength?.requiredLength;
      case Validators.minLength.name:
        return control?.validator?.(new FormControl({ length: -Infinity }))
          ?.minLength?.requiredLength;
      case Validators.min.name:
        return control?.validator?.(new FormControl(-Infinity))?.min?.min;
      case Validators.max.name:
        return control?.validator?.(new FormControl(Infinity))?.max?.max;
      default:
        return null;
    }
  }
}
