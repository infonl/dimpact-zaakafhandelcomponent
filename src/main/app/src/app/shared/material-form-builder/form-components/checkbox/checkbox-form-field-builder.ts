/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { AbstractFormFieldBuilder } from "../../model/abstract-form-field-builder";
import { CheckboxFormField } from "./checkbox-form-field";

export class CheckboxFormFieldBuilder extends AbstractFormFieldBuilder {
  readonly formField: CheckboxFormField;

  constructor(value?: any) {
    super();
    this.formField = new CheckboxFormField();
    this.formField.initControl(value);
  }
}
