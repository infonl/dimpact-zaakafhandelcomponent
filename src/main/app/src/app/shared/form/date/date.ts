/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 *
 */

import { NgIf } from "@angular/common";
import {
  booleanAttribute,
  Component,
  effect,
  input,
  signal,
} from "@angular/core";
import { AbstractControl, ReactiveFormsModule } from "@angular/forms";
import { MatButtonModule } from "@angular/material/button";
import { MatDatepickerModule } from "@angular/material/datepicker";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatIconModule } from "@angular/material/icon";
import { MatInputModule } from "@angular/material/input";
import { TranslateModule } from "@ngx-translate/core";
import moment, { Moment } from "moment";
import { takeUntil } from "rxjs";
import { CapitalizeFirstLetterPipe } from "../../pipes/capitalizeFirstLetter.pipe";
import { DagenPipe } from "../../pipes/dagen.pipe";
import { SingleInputFormField } from "../BaseFormField";
import { FormHelper } from "../helpers";

@Component({
  selector: "zac-date",
  templateUrl: "./date.html",
  styleUrls: ["./date.less"],
  standalone: true,
  imports: [
    NgIf,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatDatepickerModule,
    MatButtonModule,
    MatIconModule,
    TranslateModule,
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
   * @example `Validators.min(moment().add(1, "day").startOf("day").valueOf())`
   */
  protected readonly min = signal<Moment | null>(null);

  /**
   * To set the maximum date for the datepicker use the `Validators.max` property.
   *
   * @example `Validators.max(moment().add(1, "day").endOf("day").valueOf())`
   */
  protected readonly max = signal<Moment | null>(null);

  constructor() {
    super();

    effect(() => {
      const control = this.control();
      if (!control) {
        this.min.set(null);
        this.max.set(null);
        return;
      }

      this.updateDateBounds(control);
      control.statusChanges
        .pipe(takeUntil(this.destroy$))
        .subscribe(() => this.updateDateBounds(control));
    });
  }

  private updateDateBounds(control: AbstractControl) {
    const min = FormHelper.getValidatorValue("min", control);
    const max = FormHelper.getValidatorValue("max", control);
    this.min.set(min ? moment(min) : null);
    this.max.set(max ? moment(max) : null);
  }
}
