/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 *
 */

import { LowerCasePipe, NgFor, NgIf } from "@angular/common";
import { Component, effect, signal } from "@angular/core";
import { AbstractControl, ReactiveFormsModule } from "@angular/forms";
import { MatAutocompleteModule } from "@angular/material/autocomplete";
import { MatButtonModule } from "@angular/material/button";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatIconModule } from "@angular/material/icon";
import { MatInputModule } from "@angular/material/input";
import { MatProgressSpinnerModule } from "@angular/material/progress-spinner";
import { TranslateModule } from "@ngx-translate/core";
import { takeUntil } from "rxjs";
import { CapitalizeFirstLetterPipe } from "../../pipes/capitalizeFirstLetter.pipe";
import { MultiInputFormField } from "../BaseFormField";

@Component({
  selector: "zac-auto-complete",
  templateUrl: "./auto-complete.html",
  standalone: true,
  imports: [
    NgIf,
    NgFor,
    LowerCasePipe,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatAutocompleteModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    TranslateModule,
    CapitalizeFirstLetterPipe,
  ],
})
export class ZacAutoComplete<
  Form extends Record<string, AbstractControl>,
  Key extends keyof Form,
  Option extends Form[Key]["value"],
  OptionDisplayValue extends keyof Option | ((option: Option) => string),
> extends MultiInputFormField<Form, Key, Option, OptionDisplayValue> {
  protected filteredOptions = signal<Array<Option>>([]);

  constructor() {
    super();

    effect(() => {
      const control = this.control();
      if (!control) return;

      control.valueChanges.pipe(takeUntil(this.destroy$)).subscribe((value) => {
        if (!value) {
          this.filteredOptions.set(this.availableOptions());
          return;
        }

        const valueToFilter =
          typeof value === "string" ? value : this.displayWith(value);

        if (!valueToFilter) {
          this.filteredOptions.set(this.availableOptions());
          return;
        }

        const options = this.availableOptions();
        this.filteredOptions.set(
          options.filter((option) =>
            this.displayWith(option)
              ?.toLowerCase()
              .includes(valueToFilter.toLowerCase()),
          ),
        );
      });
    });

    effect(() => {
      this.filteredOptions.set(this.availableOptions());
    });
  }

  reset() {
    this.control()?.reset();
    this.control()?.setValue(null, { emitModelToViewChange: true });
    this.filteredOptions.set(this.availableOptions());
  }
}
