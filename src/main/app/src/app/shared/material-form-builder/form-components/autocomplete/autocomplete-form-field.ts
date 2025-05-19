/*
 * SPDX-FileCopyrightText: 2021 - 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { AbstractChoicesFormField } from "../../model/abstract-choices-form-field";
import { FieldType } from "../../model/field-type.enum";
import {Observable} from "rxjs";

export class AutocompleteFormField<T extends Record<string, unknown> | Observable<Record<string, unknown>> = Record<string, unknown>> extends AbstractChoicesFormField<T> {
  fieldType = FieldType.AUTOCOMPLETE;
  maxlength: number;

  constructor() {
    super();
  }
}
