/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 *
 */

import { Component, input } from "@angular/core";
import { AbstractControl } from "@angular/forms";
import { MultiInputFormField } from "../BaseFormField";

@Component({
  selector: "zac-select",
  templateUrl: "./select.html",
})
export class ZacSelect<
  Form extends Record<string, AbstractControl>,
  Key extends keyof Form,
  Option extends Form[Key]["value"],
  OptionDisplayValue extends keyof Option | ((option: Option) => string),
  Compare extends (a: Option, b: Option) => boolean,
> extends MultiInputFormField<Form, Key, Option, OptionDisplayValue> {
  protected readonly compare = input<Compare>();

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
    if (!suffix) return null;
    if (suffix in option) return option[suffix];

    return suffix;
  };

  // Needs to be an arrow function to de-link the reference to `this`
  // when used in the template `[compareWith]="compareWith"`
  protected compareWith = (a: Option, b: Option) => {
    const compare = this.compare();
    if (compare) return compare.call(this, a, b);

    if (this.optionDisplayValue())
      return this.displayWith(a) === this.displayWith(b);

    return a === b;
  };
}
