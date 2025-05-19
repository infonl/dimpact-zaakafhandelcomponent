/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { AbstractChoicesFormFieldBuilder } from "../../model/abstract-choices-form-field-builder";
import { SelectFormField } from "./select-form-field";

export class SelectFormFieldBuilder<T extends Record<string, unknown> = Record<string, unknown>> extends AbstractChoicesFormFieldBuilder<T> {
  readonly formField: SelectFormField<T>;

  constructor(value?: T | null) {
    super();
    this.formField = new SelectFormField();
    this.formField.initControl(value);
  }
}
