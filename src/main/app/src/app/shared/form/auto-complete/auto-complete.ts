/*
 * SPDX-FileCopyrightText: 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 *
 */

import { Component, Input, OnInit } from "@angular/core";
import { AbstractControl, FormGroup } from "@angular/forms";
import { isObservable, Observable } from "rxjs";
import { getErrorMessage } from "../helpers";

@Component({
  selector: "zac-auto-compete",
  templateUrl: "./auto-complete.html",
})
export class ZacAutoComplete<
  Form extends Record<string, AbstractControl>,
  Key extends keyof Form,
  Option extends Form[Key]["value"],
  OptionLabel extends keyof Option | ((option: Option) => string),
> implements OnInit
{
  @Input({ required: true }) key!: Key;
  @Input({ required: true }) form!: FormGroup<Form>;
  @Input({ required: true }) options!:
    | Observable<Array<Option>>
    | Array<Option>;
  @Input({ required: true }) optionLabel!: OptionLabel;

  protected control?: AbstractControl;

  private toFilterOptions: unknown[] = [];
  protected filteredOptions: unknown[] = [];

  ngOnInit() {
    this.control = this.form.get(String(this.key))!;

    if (isObservable(this.options)) {
      this.options.subscribe((options) => {
        this.toFilterOptions = this.filteredOptions = options;
      });
    } else {
      this.toFilterOptions = this.filteredOptions = this.options;
    }

    this.control.valueChanges.subscribe((value) => {
      this.filteredOptions = this.toFilterOptions.filter((option) => {
        return this.getOptionLabel(option as Option)?.includes(value) ?? true;
      });
    });
  }

  reset() {
    this.control?.reset();
    this.control?.setValue(undefined, { emitModelToViewChange: true });
    this.filteredOptions = this.toFilterOptions;
  }

  // Needs to be an arrow function in order to de-link the reference to `this`
  // when used in the template `[displayWith]="displayWith"`
  getOptionLabel = (option?: Option) => {
    if (!option) {
      return null;
    }

    if (typeof this.optionLabel === "function") {
      return this.optionLabel(option);
    }

    return String(option[this.optionLabel as keyof Option]);
  };

  protected getErrorMessage = () => getErrorMessage(this.control);
}
