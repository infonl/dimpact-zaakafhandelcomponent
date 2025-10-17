/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Observable } from "rxjs";
import { AbstractChoicesFormField } from "../../model/abstract-choices-form-field";
import { FieldType } from "../../model/field-type.enum";

type AllowedValue = Record<string, unknown> | string | string[];
type ValueType = AllowedValue | Observable<AllowedValue>;

export class SelectFormField<
  T extends ValueType = Record<string, unknown>,
> extends AbstractChoicesFormField<T> {
  fieldType = FieldType.SELECT;

  constructor() {
    super();
  }
}
