/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 *
 */

import { booleanAttribute, Component, Input, OnInit } from "@angular/core";
import { AbstractControl, FormGroup, Validators } from "@angular/forms";
import { TranslateService } from "@ngx-translate/core";
import { FormHelper } from "../helpers";

@Component({
  selector: "zac-input",
  templateUrl: "./input.html",
})
export class ZacInput<
  Form extends Record<string, AbstractControl>,
  Key extends keyof Form,
  Option extends Form[Key]["value"],
  DisplayValue extends keyof Option | ((option: Option) => string),
> implements OnInit
{
  @Input({ required: true }) key!: Key & string;
  @Input({ required: true }) form!: FormGroup<Form>;
  @Input({ transform: booleanAttribute }) readonly = false;
  @Input() type: "text" | "number" = "text";
  @Input() label?: string;
  /**
   * When a `displayValue` is declared the `input` will be hidden, and it will use an overlay for the value.
   * The input will also be put in an `readonly` mode.
   */
  @Input() displayValue?: DisplayValue;

  protected control?: AbstractControl<Option>;
  protected maxlength: number | null = null;
  protected min?: number | null = null;
  protected max?: number | null = null;

  constructor(private readonly translateService: TranslateService) {}

  ngOnInit() {
    this.control = this.form.get(String(this.key))!;
    this.maxlength = FormHelper.getValidatorValue("maxLength", this.control);
    this.min = FormHelper.getValidatorValue("min", this.control);
    this.max = FormHelper.getValidatorValue("max", this.control);

    if (Number.isFinite(this.min) || Number.isInteger(this.max)) {
      this.type = "number";
    }
  }

  protected getDisplayValue = (option?: Option) => {
    if (!option) return null;

    switch (typeof this.displayValue) {
      case "undefined":
        return String(option);
      case "function":
        return this.displayValue(option);
      default:
        return String(option[this.displayValue as keyof Option]);
    }
  };

  protected get required() {
    return this.control?.hasValidator(Validators.required) ?? false;
  }

  protected getErrorMessage = () =>
    FormHelper.getErrorMessage(this.control, this.translateService);
}
