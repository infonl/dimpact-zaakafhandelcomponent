/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 *
 */

import { Component, Input, OnInit } from "@angular/core";
import { AbstractControl, FormGroup, Validators } from "@angular/forms";
import { TranslateService } from "@ngx-translate/core";
import { FormHelper } from "../helpers";

@Component({
  selector: "zac-toggle",
  templateUrl: "./toggle.html",
  styleUrls: ["./toggle.less"],
})
export class ZacToggle<
  Form extends Record<string, AbstractControl>,
  Key extends keyof Form,
> implements OnInit
{
  @Input({ required: true }) key!: Key & string;
  @Input({ required: true }) form!: FormGroup<Form>;
  @Input() label?: string;
  @Input() labelPosition: "before" | "after" = "after";

  protected control?: AbstractControl<boolean>;

  constructor(private readonly translateService: TranslateService) {}

  ngOnInit() {
    this.control = this.form.get(String(this.key))!;
  }

  protected get required() {
    return this.control?.hasValidator(Validators.required) ?? false;
  }

  protected getErrorMessage = () =>
    FormHelper.getErrorMessage(this.control, this.translateService);
}
