/*
 * SPDX-FileCopyrightText: 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 *
 */

import { Component, Input, numberAttribute, OnInit } from "@angular/core";
import { AbstractControl, FormGroup } from "@angular/forms";
import { MatInput } from "@angular/material/input";
import { TranslateService } from "@ngx-translate/core";
import { CustomValidators } from "../helpers";

@Component({
  selector: "zac-input",
  templateUrl: "./input.html",
})
export class ZacInput<
    Form extends Record<string, AbstractControl>,
    Key extends keyof Form,
  >
  extends MatInput
  implements OnInit
{
  @Input({ required: true }) key!: Key;
  @Input({ required: true }) form!: FormGroup<Form>;
  @Input({ transform: numberAttribute }) maxlength?: number;

  protected control?: AbstractControl<string>;

  constructor(private readonly translateService: TranslateService) {
    super();
  }

  ngOnInit() {
    this.control = this.form.get(String(this.key))!;
  }

  protected getErrorMessage = () =>
    CustomValidators.getErrorMessage(this.control, this.translateService);
}
