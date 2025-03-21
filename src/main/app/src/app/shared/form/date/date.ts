/*
 * SPDX-FileCopyrightText: <YYYY> Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 *
 */

import { booleanAttribute, Component, Input, OnInit } from "@angular/core";
import { AbstractControl, FormGroup } from "@angular/forms";
import { MatDatepickerInput } from "@angular/material/datepicker";
import { TranslateService } from "@ngx-translate/core";
import moment from "moment";
import { CustomValidators } from "../helpers";

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

  constructor(private readonly translateService: TranslateService) {
    super();
  }

  ngOnInit() {
    this.control = this.form.get(String(this.key))!;
  }

  protected getErrorMessage = () =>
    CustomValidators.getErrorMessage(this.control, this.translateService);

  protected readonly console = console;
}
