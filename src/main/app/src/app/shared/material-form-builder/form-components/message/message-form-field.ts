/*
 * SPDX-FileCopyrightText: 2023 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { TextIcon } from "../../../edit/text-icon";
import { AbstractFormControlField } from "../../model/abstract-form-control-field";
import { FieldType } from "../../model/field-type.enum";

export class MessageFormField extends AbstractFormControlField {
  fieldType = FieldType.MESSAGE;
  icon: TextIcon;

  constructor() {
    super();
  }
}
