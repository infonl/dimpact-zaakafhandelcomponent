/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 *
 */

import {
  booleanAttribute,
  Component,
  effect,
  input,
  signal,
  WritableSignal,
} from "@angular/core";
import { AbstractControl, ReactiveFormsModule } from "@angular/forms";
import { MatButtonModule } from "@angular/material/button";
import { MatDatepickerModule } from "@angular/material/datepicker";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatIconModule } from "@angular/material/icon";
import { MatInputModule } from "@angular/material/input";
import { TranslatePipe } from "@ngx-translate/core";
import { fromTimestamp, PickedDate } from "../../../date-time";
import { CapitalizeFirstLetterPipe } from "../../pipes/capitalizeFirstLetter.pipe";
import { DagenPipe } from "../../pipes/dagen.pipe";
import { SingleInputFormField } from "../BaseFormField";
import { FormHelper } from "../helpers";

@Component({
  selector: "zac-date",
  templateUrl: "./date.html",
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatDatepickerModule,
    MatButtonModule,
    MatIconModule,
    TranslatePipe,
    CapitalizeFirstLetterPipe,
    DagenPipe,
  ],
})
export class ZacDate<
  Form extends Record<string, AbstractControl>,
  Key extends keyof Form,
  Option extends Form[Key]["value"],
> extends SingleInputFormField<Form, Key, Option> {
  protected showAmountOfDays = input(false, { transform: booleanAttribute });

  /**
   * To set the minimum date for the datepicker use the `Validators.min` property.
   *
   * @example `Validators.min(startOfDay(addDays(now(), 1)).valueOf())`
   */
  protected readonly min = signal<PickedDate | null>(null);

  /**
   * To set the maximum date for the datepicker use the `Validators.max` property.
   *
   * @example `Validators.max(addDays(today(), 1).valueOf())`
   */
  protected readonly max = signal<PickedDate | null>(null);

  constructor() {
    super();

    effect((onCleanup) => {
      const control = this.control();
      if (!control) {
        this.min.set(null);
        this.max.set(null);
        return;
      }

      this.updateDateBounds(control);
      const subscription = control.statusChanges.subscribe(() =>
        this.updateDateBounds(control),
      );
      onCleanup(() => subscription.unsubscribe());
    });
  }

  private updateDateBounds(control: AbstractControl) {
    this.setBoundIfChanged(
      this.min,
      FormHelper.getValidatorValue("min", control),
    );
    this.setBoundIfChanged(
      this.max,
      FormHelper.getValidatorValue("max", control),
    );
  }

  private setBoundIfChanged(
    bound: WritableSignal<PickedDate | null>,
    value: number | null,
  ) {
    if (value === (bound()?.valueOf() ?? null)) return;
    bound.set(value ? fromTimestamp(value) : null);
  }
}
