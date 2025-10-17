/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { AbstractFormControlField } from "../../model/abstract-form-control-field";
import { FieldType } from "../../model/field-type.enum";

export class CheckboxFormField<
  T extends boolean = boolean,
> extends AbstractFormControlField<T> {
  fieldType = FieldType.CHECKBOX;

  constructor() {
    super();
  }
}
