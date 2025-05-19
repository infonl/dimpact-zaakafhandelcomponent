/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Observable } from "rxjs";
import { AbstractChoicesFormFieldBuilder } from "../../model/abstract-choices-form-field-builder";
import { RadioFormField } from "./radio-form-field";

type AllowedValue = Record<string, unknown> | string;
type ValueType = AllowedValue | Observable<AllowedValue>;

export class RadioFormFieldBuilder<
  T extends ValueType = Record<string, unknown>,
> extends AbstractChoicesFormFieldBuilder<T> {
  readonly formField: RadioFormField<T>;

  constructor(value?: T | null) {
    super();
    this.formField = new RadioFormField();
    this.formField.initControl(value);
  }
}
