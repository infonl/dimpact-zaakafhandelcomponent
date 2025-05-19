/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { AbstractChoicesFormFieldBuilder } from "../../model/abstract-choices-form-field-builder";
import { RadioFormField } from "./radio-form-field";

export class RadioFormFieldBuilder<T extends Record<string, unknown> = Record<string, unknown>> extends AbstractChoicesFormFieldBuilder<T> {
  readonly formField: RadioFormField<T>;

  constructor(value?: T) {
    super();
    this.formField = new RadioFormField();
    this.formField.initControl(value);
  }
}
