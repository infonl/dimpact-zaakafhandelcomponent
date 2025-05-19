/*
 * SPDX-FileCopyrightText: 2021 - 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { AbstractChoicesFormFieldBuilder } from "../../model/abstract-choices-form-field-builder";
import { AutocompleteFormField } from "./autocomplete-form-field";
import {Observable} from "rxjs";

export class AutocompleteFormFieldBuilder<T extends Record<string, unknown> | Observable<Record<string, unknown>> = Record<string, unknown>> extends AbstractChoicesFormFieldBuilder<T> {
  readonly formField: AutocompleteFormField<T>;

  constructor(value?: T) {
    super();
    this.formField = new AutocompleteFormField();
    this.formField.initControl(value);
  }

  maxlength(maxlength: number): this {
    this.formField.maxlength = maxlength;
    return this;
  }
}
