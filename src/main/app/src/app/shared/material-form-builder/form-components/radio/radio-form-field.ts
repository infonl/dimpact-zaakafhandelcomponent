/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { AbstractChoicesFormField } from "../../model/abstract-choices-form-field";
import { FieldType } from "../../model/field-type.enum";

export class RadioFormField extends AbstractChoicesFormField {
  fieldType = FieldType.RADIO;

  constructor() {
    super();
  }
}
