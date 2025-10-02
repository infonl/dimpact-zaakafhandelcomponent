/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 *
 */

import {
  Component, computed, input,
  numberAttribute,
} from "@angular/core";
import { AbstractControl } from "@angular/forms";
import { FormHelper } from "../helpers";
import {SingleInputFormField} from "../BaseFormField";

@Component({
  selector: "zac-textarea",
  templateUrl: "./textarea.html",
})
export class ZacTextarea<
  Form extends Record<string, AbstractControl>,
  Key extends keyof Form,
    Option extends Form[Key]["value"],
> extends SingleInputFormField<Form, Key, Option>
{
  protected minRows = input(5, { transform: numberAttribute });
    protected maxRows = input(15, { transform: numberAttribute });

  protected maxlength = computed(() => FormHelper.getValidatorValue("maxLength", this.control() ?? null));
}
