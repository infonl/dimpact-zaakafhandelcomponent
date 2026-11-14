/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { AbstractFormControlField } from "../../model/abstract-form-control-field";
import { FieldType } from "../../model/field-type.enum";

export class HiddenFormField extends AbstractFormControlField {
  fieldType: FieldType = FieldType.HIDDEN;

  constructor() {
    super();
  }

  initControl(value?: any) {
    super.initControl(value);
    this.label = "hidden";
  }
}
