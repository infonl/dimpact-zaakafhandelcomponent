/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 *
 */

import { booleanAttribute, Component, computed, input } from "@angular/core";
import { AbstractControl } from "@angular/forms";
import moment from "moment";
import { SingleInputFormField } from "../BaseFormField";
import { FormHelper } from "../helpers";

@Component({
  selector: "zac-date",
  templateUrl: "./date.html",
  styleUrls: ["./date.less"],
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
