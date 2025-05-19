/*
 * SPDX-FileCopyrightText: 2021 - 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Observable } from "rxjs";
import { AbstractChoicesFormFieldBuilder } from "../../model/abstract-choices-form-field-builder";
import { AutocompleteFormField } from "./autocomplete-form-field";

type AllowedValue = Record<string, unknown>;
type ValueType = AllowedValue | Observable<AllowedValue>;

export class AutocompleteFormFieldBuilder<
  T extends ValueType = Record<string, unknown>,
> extends AbstractChoicesFormFieldBuilder<T> {
  readonly formField: AutocompleteFormField<T>;

  constructor(value?: T | null) {
    super();
    this.formField = new AutocompleteFormField();
    this.formField.initControl(value);
  }

  maxlength(maxlength: number): this {
    this.formField.maxlength = maxlength;
    return this;
  }
}
