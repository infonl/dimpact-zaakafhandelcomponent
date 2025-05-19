/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { AbstractFormFieldBuilder } from "../../model/abstract-form-field-builder";
import { CheckboxFormField } from "./checkbox-form-field";

export class CheckboxFormFieldBuilder<T extends boolean = boolean> extends AbstractFormFieldBuilder<T> {
  readonly formField: CheckboxFormField<T>;

  constructor(value?: T) {
    super();
    this.formField = new CheckboxFormField();
    this.formField.initControl(value);
  }
}
