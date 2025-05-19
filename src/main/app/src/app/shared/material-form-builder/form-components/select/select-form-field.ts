/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { AbstractChoicesFormField } from "../../model/abstract-choices-form-field";
import { FieldType } from "../../model/field-type.enum";

export class SelectFormField<T extends Record<string, unknown> = Record<string, unknown>> extends AbstractChoicesFormField<T> {
  fieldType = FieldType.SELECT;

  constructor() {
    super();
  }
}
