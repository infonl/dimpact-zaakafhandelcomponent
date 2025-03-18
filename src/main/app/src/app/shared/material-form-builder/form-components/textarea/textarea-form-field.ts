/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { AbstractFormControlField } from "../../model/abstract-form-control-field";
import { FieldType } from "../../model/field-type.enum";

export class TextareaFormField extends AbstractFormControlField {
  fieldType = FieldType.TEXTAREA;
  maxlength: number;

  constructor() {
    super();
  }
}
