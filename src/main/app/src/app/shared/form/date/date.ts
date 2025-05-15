/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 *
 */

import { booleanAttribute, Component, Input, OnInit } from "@angular/core";
import { AbstractControl, FormGroup } from "@angular/forms";
import { TranslateService } from "@ngx-translate/core";
import moment from "moment";
import { FormHelper } from "../helpers";

@Component({
  selector: "zac-date",
  templateUrl: "./date.html",
})
export class ZacDate<
  Form extends Record<string, AbstractControl>,
  Key extends keyof Form,
> implements OnInit
{
  @Input({ required: true }) key!: Key & string;
  @Input({ required: true }) form!: FormGroup<Form>;
  @Input({ transform: booleanAttribute }) showAmountOfDays?: boolean;

  protected control?: AbstractControl<moment.Moment>;

  constructor(private readonly translateService: TranslateService) {}

  ngOnInit() {
    this.control = this.form.get(String(this.key))!;
  }

  /**
   * To set the minimum date for the datepicker use the `Validator.min` property.
   *
   * @example `Validators.min(moment().add(1, "day").startOf("day").valueOf())`
   */
  get min(): moment.Moment | null {
    const value = FormHelper.getValidatorValue("min", this.control);
    return value ? moment(value) : null;
  }

  /**
   * To set the maximum date for the datepicker use the `Validator.max` property.
   *
   * @example `Validators.max(moment().add(1, "day").endOf("day").valueOf())`
   */
  get max(): moment.Moment | null {
    const value = FormHelper.getValidatorValue("max", this.control);
    return value ? moment(value) : null;
  }

  protected getErrorMessage = () =>
    FormHelper.getErrorMessage(this.control, this.translateService);
}
