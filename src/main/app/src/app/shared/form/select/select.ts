/*
 * SPDX-FileCopyrightText: 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 *
 */

import {
  Component,
  Input,
  OnChanges,
  OnDestroy,
  OnInit,
  SimpleChanges,
} from "@angular/core";
import {AbstractControl, FormGroup, Validators} from "@angular/forms";
import { TranslateService } from "@ngx-translate/core";
import { Subject } from "rxjs";
import { FormHelper } from "../helpers";

@Component({
  selector: "zac-select",
  templateUrl: "./select.html"
})
export class ZacSelect<
    Form extends Record<string, AbstractControl>,
    Key extends keyof Form,
    Option extends Form[Key]["value"],
    OptionDisplayValue extends keyof Option | ((option: Option) => string),
    Compare extends (a: Option, b: Option) => boolean,
  >
  implements OnInit, OnChanges, OnDestroy
{
  @Input({ required: true }) key!: Key & string;
  @Input({ required: true }) form!: FormGroup<Form>;
  @Input({ required: true }) options!: Array<Option> | null;
  @Input() optionDisplayValue?: OptionDisplayValue;
  @Input() optionDisplayValuePrefix = ""
  @Input() compare?: Compare;
  @Input() label?: string;
  /**
   * The suffix to display after the input field.
   * It will get translated using the `translate` pipe.
   */
  @Input() suffix?: string;

  protected control?: AbstractControl<Option | null>;
  protected availableOptions: Option[] = [];

  private destroy$ = new Subject();

  constructor(private readonly translateService: TranslateService) {}

  ngOnInit() {
    this.control = this.form.get(String(this.key))!;

    this.setOptions(this.options);
  }

  ngOnChanges(changes: SimpleChanges) {
    if ("options" in changes) {
      this.setOptions(this.options);
    }
  }

  ngOnDestroy() {
    this.destroy$.next(true);
    this.destroy$.complete();
  }

  // Needs to be an arrow function in order to de-link the reference to `this`
  // when used in the template `[displayWith]="displayWith"`
  protected displayWith = (option?: Option | null) => {
    if (!option) return "";

    switch (typeof this.optionDisplayValue) {
      case "undefined":
        return String(option);
      case "function":
        return this.optionDisplayValue(option);
      default:
        return String(option[this.optionDisplayValue as keyof Option]);
    }
  };

  protected isRequired() {
    return this.control?.hasValidator(Validators.required) ?? false;
  }

  // Needs to be an arrow function in order to de-link the reference to `this`
  // when used in the template `[compareWith]="compareWith"`
  protected compareWith = (a: Option, b: Option) => {
    return this.compare?.(a, b) ?? a === b;
  };

  protected getErrorMessage = () =>
    FormHelper.getErrorMessage(this.control, this.translateService);

  protected get placeholder() {
    return this.isRequired() ? `-kies-` : "-geen-"
  }

  private setOptions(input: Array<Option> | null) {
    if (!input) return

    this.availableOptions = input;
  }
}
