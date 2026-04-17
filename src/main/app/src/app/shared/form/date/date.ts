/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 *
 */

import { NgIf } from "@angular/common";
import { booleanAttribute, Component, computed, input } from "@angular/core";
import { ReactiveFormsModule } from "@angular/forms";
import { AbstractControl } from "@angular/forms";
import { MatButtonModule } from "@angular/material/button";
import { MatDatepickerModule } from "@angular/material/datepicker";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatIconModule } from "@angular/material/icon";
import { MatInputModule } from "@angular/material/input";
import { TranslateModule } from "@ngx-translate/core";
import moment from "moment";
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
   * To set the minimum date for the datepicker use the `Validator.min` property.
   *
   * @example `Validators.min(moment().add(1, "day").startOf("day").valueOf())`
   */
  protected min = computed(() => {
    const value = FormHelper.getValidatorValue("min", this.control() ?? null);
    return value ? moment(value) : null;
  });

  /**
   * To set the maximum date for the datepicker use the `Validator.max` property.
   *
   * @example `Validators.max(moment().add(1, "day").endOf("day").valueOf())`
   */
  protected max = computed(() => {
    const value = FormHelper.getValidatorValue("max", this.control() ?? null);
    return value ? moment(value) : null;
  });
}
