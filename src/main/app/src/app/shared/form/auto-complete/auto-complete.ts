/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
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
import { AbstractControl, FormGroup } from "@angular/forms";
import { TranslateService } from "@ngx-translate/core";
import { Observable, Subject, takeUntil } from "rxjs";
import { FormHelper } from "../helpers";

@Component({
  selector: "zac-auto-complete",
  templateUrl: "./auto-complete.html",
})
export class ZacAutoComplete<
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

  protected control?: AbstractControl<Option | null>;

  private availableOptions: Option[] = [];
  protected filteredOptions: Option[] = [];

  private destroy$ = new Subject();

  constructor(private readonly translateService: TranslateService) {}

  ngOnInit() {
    this.control = this.form.get(String(this.key))!;

    this.setOptions(this.options);

    this.control.valueChanges
      .pipe(takeUntil(this.destroy$))
      .subscribe((value) => {
        this.filteredOptions = this.availableOptions.filter((option) => {
          if (!value) return true;

          const valueToFilter =
            typeof value === "string" ? value : this.displayWith(value);

          return this.displayWith(option)
            .toLowerCase()
            .includes(valueToFilter.toLowerCase());
        });
      });
  }

  ngOnChanges(changes: SimpleChanges) {
    if ("options" in changes) {
      this.setOptions(changes.options.currentValue);
    }
  }

  reset() {
    this.control?.reset();
    this.control?.setValue(null, { emitModelToViewChange: true });
    this.filteredOptions = this.availableOptions;
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

  protected getErrorMessage = () =>
    FormHelper.getErrorMessage(this.control, this.translateService);

  private setOptions(options: Array<Option> | Observable<Array<Option>> = []) {
    if (options instanceof Observable) {
      options
        .pipe(takeUntil(this.destroy$))
        .subscribe((options) => this.setOptions(options));
      return;
    }
    this.availableOptions = this.filteredOptions = options;
  }

  ngOnDestroy() {
    this.destroy$.next(true);
    this.destroy$.complete();
  }
}
