/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component } from "@angular/core";
import { AbstractControl } from "@angular/forms";
import { MultiInputFormField } from "../BaseFormField";

@Component({
  selector: "zac-radio",
  templateUrl: "./radio.html",
  standalone: false,
})
export class ZacRadio<
  Form extends Record<string, AbstractControl>,
  Key extends keyof Form,
  Option extends Form[Key]["value"],
  OptionDisplayValue extends keyof Option | ((option: Option) => string),
> extends MultiInputFormField<Form, Key, Option, OptionDisplayValue> {}
