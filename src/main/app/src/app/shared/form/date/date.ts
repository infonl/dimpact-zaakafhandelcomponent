/*
 * SPDX-FileCopyrightText: <YYYY> Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 *
 */

import { Component, Input, OnInit } from "@angular/core";
import { AbstractControl, FormGroup } from "@angular/forms";
import { getErrorMessage } from "../helpers";
import moment from "moment";

@Component({
  selector: "zac-date",
  templateUrl: "./date.html",
})
export class ZacDate<
  Form extends Record<string, AbstractControl>,
  Key extends keyof Form,
> implements OnInit
{
  @Input({ required: true }) key!: Key;
  @Input({ required: true }) form!: FormGroup<Form>;

  protected control?: AbstractControl<moment.Moment>;

  ngOnInit() {
    this.control = this.form.get(String(this.key))!;

    this.control.valueChanges.subscribe((value) => {
      console.log({ value }, value?.toDate());
    });
  }

  protected getErrorMessage = () => getErrorMessage(this.control);
}
