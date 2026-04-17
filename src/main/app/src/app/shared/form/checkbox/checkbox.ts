/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 *
 */

import { NgIf } from "@angular/common";
import { Component } from "@angular/core";
import { AbstractControl, ReactiveFormsModule } from "@angular/forms";
import { MatCheckboxModule } from "@angular/material/checkbox";
import { MatError, MatHint } from "@angular/material/form-field";
import { TranslateModule } from "@ngx-translate/core";
import { CapitalizeFirstLetterPipe } from "../../pipes/capitalizeFirstLetter.pipe";
import { SingleInputFormField } from "../BaseFormField";

@Component({
  selector: "zac-checkbox",
  templateUrl: "./checkbox.html",
  styleUrls: ["./checkbox.less"],
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatCheckboxModule,
    MatError,
    MatHint,
    NgIf,
    TranslateModule,
    CapitalizeFirstLetterPipe,
  ],
})
export class ZacCheckbox<
  Form extends Record<string, AbstractControl>,
  Key extends keyof Form,
  Option extends Form[Key]["value"],
> extends SingleInputFormField<Form, Key, Option> {}
