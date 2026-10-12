/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 *
 */

import { LowerCasePipe } from "@angular/common";
import { Component, input } from "@angular/core";
import { AbstractControl, ReactiveFormsModule } from "@angular/forms";
import { MatButtonModule } from "@angular/material/button";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatInputModule } from "@angular/material/input";
import { MatProgressSpinnerModule } from "@angular/material/progress-spinner";
import { MatSelectModule } from "@angular/material/select";
import { TranslateModule } from "@ngx-translate/core";
import { CapitalizeFirstLetterPipe } from "../../pipes/capitalizeFirstLetter.pipe";
import { toI18nKey } from "../../utils/i18n-key";
import { MultiInputFormField } from "../BaseFormField";

@Component({
  selector: "zac-select",
  templateUrl: "./select.html",
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    TranslateModule,
    CapitalizeFirstLetterPipe,
    LowerCasePipe,
  ],
})
export class ZacSelect<
  Form extends Record<string, AbstractControl>,
  Key extends keyof Form,
  Option extends Form[Key]["value"],
  OptionDisplayValue extends keyof Option | ((option: Option) => string),
> extends MultiInputFormField<Form, Key, Option, OptionDisplayValue> {
  /**
   * The suffix to display after the input field.
   * It will get translated using the `translate` pipe.
   *
   * - If the suffix is a `Key` of the `Option` type, it will display the value of that key.
   * - If the suffix is a string, it will display that string.
   */
  protected readonly suffix = input<string>();

  protected displaySuffix = (option: Option) => {
    const suffix = this.suffix();
    if (suffix && suffix in option) return option[suffix];

    return null;
  };

  protected suffixTranslationKey = (option: Option) => {
    const suffix = this.displaySuffix(option);
    return suffix ? toI18nKey(String(suffix)) : null;
  };
}
