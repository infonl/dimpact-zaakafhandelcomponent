/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { AbstractFormControlField } from "../../model/abstract-form-control-field";
import { FieldType } from "../../model/field-type.enum";

export class HiddenFormField<T = unknown> extends AbstractFormControlField<T> {
  fieldType: FieldType = FieldType.HIDDEN;

  constructor() {
    super();
  }

  initControl(value?: T) {
    super.initControl(value);
    this.label = "hidden";
  }
}
