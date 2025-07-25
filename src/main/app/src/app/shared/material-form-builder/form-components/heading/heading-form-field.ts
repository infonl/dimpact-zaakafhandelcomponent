/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { AbstractFormControlField } from "../../model/abstract-form-control-field";
import { FieldType } from "../../model/field-type.enum";

export class HeadingFormField extends AbstractFormControlField {
  fieldType = FieldType.HEADING;

  level: HeadingLevel = HeadingLevel.H1;

  constructor() {
    super();
  }
}

export enum HeadingLevel {
  H1 = "H1",
  H2 = "H2",
  H3 = "H3",
  H4 = "H4",
  H5 = "H5",
  H6 = "H6",
}
