/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 *
 */

import {
  Component, effect, signal,
} from "@angular/core";
import { AbstractControl } from "@angular/forms";
import {MultiInputFormField} from "../BaseFormField";
import {takeUntil} from "rxjs";

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
  extends MultiInputFormField<Form, Key, Option, OptionDisplayValue>
{
  protected filteredOptions = signal<Array<Option>>([]);

  constructor() {
    super();

    effect(() => {
      const control = this.control();
        if (!control) return;

        control.valueChanges.pipe(takeUntil(this.destroy$)).subscribe((value) => {
          if(!value) {
            this.filteredOptions.set(this.availableOptions())
            return
          }

            const valueToFilter =
                typeof value === "string" ? value : this.displayWith(value);

            if(!valueToFilter) {
                this.filteredOptions.set(this.availableOptions())
                return
            }

            const options = this.availableOptions();
            this.filteredOptions.set(options.filter((option) =>
                this.displayWith(option)?.toLowerCase().includes(valueToFilter.toLowerCase())
            ));
        })
    }, {
      allowSignalWrites: true
    });

    effect(() => {
      this.filteredOptions.set(this.availableOptions());
    }, { allowSignalWrites: true})
  }

  reset() {
    this.control()?.reset();
    this.control()?.setValue(null, { emitModelToViewChange: true });
    this.filteredOptions.set(this.availableOptions());
  }
}
