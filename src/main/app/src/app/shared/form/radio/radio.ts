/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  Component,
  Input,
  OnChanges,
  OnDestroy,
  OnInit,
  SimpleChanges,
} from "@angular/core";
import { AbstractControl, FormGroup, Validators } from "@angular/forms";
import { TranslateService } from "@ngx-translate/core";
import { Observable, Subject, takeUntil } from "rxjs";
import { FormHelper } from "../helpers";

@Component({
  selector: "zac-radio",
  templateUrl: "./radio.html",
})
export class ZacRadio<
    Form extends Record<string, AbstractControl>,
    Key extends keyof Form,
    Option extends Form[Key]["value"],
    OptionDisplayValue extends keyof Option | ((option: Option) => string),
  >
  implements OnInit, OnChanges, OnDestroy
{
  @Input({ required: true }) key!: Key & string;
  @Input({ required: true }) form!: FormGroup<Form>;
  @Input({ required: true }) options!:
    | Array<Option>
    | Observable<Array<Option>>;
  @Input() optionDisplayValue?: OptionDisplayValue;
  @Input() label?: string;

  protected control?: AbstractControl<Option | null>;
  protected availableOptions: Option[] = [];

  private destroy$ = new Subject();

  constructor(private readonly translateService: TranslateService) {}

  ngOnInit() {
    this.control = this.form.get(String(this.key))!;
    console.log(this.control.value);
    this.setOptions(this.options);
  }

  ngOnChanges(changes: SimpleChanges) {
    if ("options" in changes) {
      this.setOptions(changes.options.currentValue);
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

  protected getErrorMessage = () =>
    FormHelper.getErrorMessage(this.control, this.translateService);

  private setOptions(options: Array<Option> | Observable<Array<Option>> = []) {
    if (options instanceof Observable) {
      options
        .pipe(takeUntil(this.destroy$))
        .subscribe((options) => this.setOptions(options));
      return;
    }

    this.availableOptions = options;
  }
}
