/*
 * SPDX-FileCopyrightText: <YYYY> Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 *
 */

import {
  booleanAttribute,
  Component,
  ElementRef,
  Input,
  OnInit,
  Optional,
} from "@angular/core";
import { AbstractControl, FormGroup } from "@angular/forms";
import { DateAdapter } from "@angular/material/core";
import { MatDatepickerInput } from "@angular/material/datepicker";
import { MatFormField } from "@angular/material/form-field";
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
  >
  extends MatDatepickerInput<moment.Moment>
  implements OnInit
{
  @Input({ required: true }) key!: Key;
  @Input({ required: true }) form!: FormGroup<Form>;
  @Input({ transform: booleanAttribute }) showAmountOfDays?: boolean;

  protected control?: AbstractControl<moment.Moment>;

  constructor(
    elementRef: ElementRef<HTMLInputElement>,
    dateAdapter: DateAdapter<moment.Moment>,
    @Optional() _formField: MatFormField,
    private readonly translateService: TranslateService,
  ) {
    super(
      elementRef,
      dateAdapter,
      {
        parse: {
          dateInput: "yyyy-MM-DD",
        },
        display: {
          dateInput: "yyyy-MM-DD",
          monthYearLabel: "MMMM YYYY",
          dateA11yLabel: "LL",
          monthYearA11yLabel: "MMMM YYYY",
        },
      },
      _formField,
    );
  }

  ngOnInit() {
    this.control = this.form.get(String(this.key))!;
  }

  /**
   * To set the minimum date for the datepicker use the `Validator.min` property.
   *
   * @example `Validators.min(moment().add(1, "day").startOf("day").valueOf())`
   */
  override get min(): moment.Moment | null {
    return moment(FormHelper.getValidatorValue("min", this.control));
  }

  /**
   * To set the maximum date for the datepicker use the `Validator.max` property.
   *
   * @example `Validators.max(moment().add(1, "day").endOf("day").valueOf())`
   */
  override get max(): moment.Moment | null {
    return moment(FormHelper.getValidatorValue("max", this.control));
  }

  protected getErrorMessage = () =>
    FormHelper.getErrorMessage(this.control, this.translateService);
}
