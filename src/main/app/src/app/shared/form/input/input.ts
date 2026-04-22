/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 *
 */

import { NgClass, NgIf } from "@angular/common";
import { booleanAttribute, Component, computed, input } from "@angular/core";
import { AbstractControl, ReactiveFormsModule } from "@angular/forms";
import { MatButtonModule } from "@angular/material/button";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatIconModule } from "@angular/material/icon";
import { MatInputModule } from "@angular/material/input";
import { TranslateModule } from "@ngx-translate/core";
import { CapitalizeFirstLetterPipe } from "../../pipes/capitalizeFirstLetter.pipe";
import { SingleInputFormField } from "../BaseFormField";
import { FormHelper } from "../helpers";

@Component({
  selector: "zac-input",
  templateUrl: "./input.html",
  standalone: true,
  imports: [
    NgIf,
    NgClass,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    TranslateModule,
    CapitalizeFirstLetterPipe,
  ],
})
export class ZacInput<
  Form extends Record<string, AbstractControl>,
  Key extends keyof Form,
  Option extends Form[Key]["value"],
  DisplayValue extends keyof Option | ((option: Option) => string),
> extends SingleInputFormField<Form, Key, Option> {
  public readonly readonly = input(false, { transform: booleanAttribute });
  public readonly type = input<"text" | "number">("text");

  /**
   * When a `displayValue` is declared, the `input` will be hidden, and it will use an overlay for the value.
   * The input will also be put in a `readonly` mode.
   */
  public displayValue = input<DisplayValue>();

  protected readonly maxlength = computed(() =>
    FormHelper.getValidatorValue("maxLength", this.control() ?? null),
  );
  protected readonly min = computed(() =>
    FormHelper.getValidatorValue("min", this.control() ?? null),
  );
  protected readonly max = computed(() =>
    FormHelper.getValidatorValue("max", this.control() ?? null),
  );

  protected readonly computedType = computed(() => {
    const baseType = this.type();
    if (baseType === "number") return "number";

    if (Number.isFinite(this.min()) || Number.isFinite(this.max()))
      return "number";
    return baseType;
  });

  protected getDisplayValue = (option?: Option | null) => {
    if (!option) return null;

    const displayValue = this.displayValue();
    switch (typeof displayValue) {
      case "undefined":
        return String(option);
      case "function":
        return displayValue.call(this, option);
      default:
        return String(option[displayValue as unknown as keyof Option]);
    }
  };
}
