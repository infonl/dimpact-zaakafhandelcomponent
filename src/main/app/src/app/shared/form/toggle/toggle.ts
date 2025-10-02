/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 *
 */

import {Component, input} from "@angular/core";
import { AbstractControl } from "@angular/forms";
import {SingleInputFormField} from "../BaseFormField";
import {MatSlideToggle} from "@angular/material/slide-toggle";

@Component({
  selector: "zac-toggle",
  templateUrl: "./toggle.html",
  styleUrls: ["./toggle.less"],
})
export class ZacToggle<
  Form extends Record<string, AbstractControl>,
  Key extends keyof Form,
    Option extends Form[Key]["value"],
> extends SingleInputFormField<Form, Key, Option>
{
  protected labelPosition = input<MatSlideToggle['labelPosition']>("after");
}
