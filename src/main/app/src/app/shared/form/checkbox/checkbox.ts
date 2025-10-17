/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 *
 */

import { Component } from "@angular/core";
import { AbstractControl } from "@angular/forms";
import { SingleInputFormField } from "../BaseFormField";

@Component({
  selector: "zac-checkbox",
  templateUrl: "./checkbox.html",
  styleUrls: ["./checkbox.less"],
})
export class ZacCheckbox<
  Form extends Record<string, AbstractControl>,
  Key extends keyof Form,
  Option extends Form[Key]["value"],
> extends SingleInputFormField<Form, Key, Option> {}
