/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 *
 */

import { NgIf } from "@angular/common";
import { Component, input } from "@angular/core";
import { AbstractControl, ReactiveFormsModule } from "@angular/forms";
import { MatError, MatHint, MatLabel } from "@angular/material/form-field";
import { MatSlideToggle, MatSlideToggleModule } from "@angular/material/slide-toggle";
import { TranslateModule } from "@ngx-translate/core";
import { CapitalizeFirstLetterPipe } from "../../pipes/capitalizeFirstLetter.pipe";
import { SingleInputFormField } from "../BaseFormField";

@Component({
  selector: "zac-toggle",
  templateUrl: "./toggle.html",
  styleUrls: ["./toggle.less"],
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatSlideToggleModule,
    MatError,
    MatHint,
    MatLabel,
    NgIf,
    TranslateModule,
    CapitalizeFirstLetterPipe,
  ],
})
export class ZacToggle<
  Form extends Record<string, AbstractControl>,
  Key extends keyof Form,
  Option extends Form[Key]["value"],
> extends SingleInputFormField<Form, Key, Option> {
  protected labelPosition = input<MatSlideToggle["labelPosition"]>("after");
}
