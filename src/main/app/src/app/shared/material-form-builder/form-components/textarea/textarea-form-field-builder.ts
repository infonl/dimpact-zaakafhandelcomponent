/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { AbstractFormFieldBuilder } from "../../model/abstract-form-field-builder";
import { TextareaFormField } from "./textarea-form-field";

export class TextareaFormFieldBuilder extends AbstractFormFieldBuilder {
  readonly formField: TextareaFormField;

  constructor(value?: any) {
    super();
    this.formField = new TextareaFormField();
    this.formField.initControl(value);
  }

  maxlength(maxlength: number): this {
    this.formField.maxlength = maxlength;
    return this;
  }

  disabled(disable: boolean = true) {
    if (disable) {
      this.formField.formControl.disable();
    }
    return this;
  }
}
