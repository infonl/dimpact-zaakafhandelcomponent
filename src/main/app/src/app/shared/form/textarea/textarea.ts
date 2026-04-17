/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 *
 */

import { CdkTextareaAutosize } from "@angular/cdk/text-field";
import { NgIf } from "@angular/common";
import { Component, computed, input, numberAttribute } from "@angular/core";
import { AbstractControl, ReactiveFormsModule } from "@angular/forms";
import { MatButtonModule } from "@angular/material/button";
import {
  MatError,
  MatFormFieldModule,
  MatHint,
  MatLabel,
  MatSuffix,
} from "@angular/material/form-field";
import { MatIconModule } from "@angular/material/icon";
import { MatInputModule } from "@angular/material/input";
import { TranslateModule } from "@ngx-translate/core";
import { CapitalizeFirstLetterPipe } from "../../pipes/capitalizeFirstLetter.pipe";
import { SingleInputFormField } from "../BaseFormField";
import { FormHelper } from "../helpers";

@Component({
  selector: "zac-textarea",
  templateUrl: "./textarea.html",
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatFormFieldModule,
    MatLabel,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatSuffix,
    MatError,
    MatHint,
    CdkTextareaAutosize,
    NgIf,
    TranslateModule,
    CapitalizeFirstLetterPipe,
  ],
})
export class ZacTextarea<
  Form extends Record<string, AbstractControl>,
  Key extends keyof Form,
  Option extends Form[Key]["value"],
> extends SingleInputFormField<Form, Key, Option> {
  protected minRows = input(5, { transform: numberAttribute });
  protected maxRows = input(15, { transform: numberAttribute });

  protected maxlength = computed(() =>
    FormHelper.getValidatorValue("maxLength", this.control() ?? null),
  );
}
