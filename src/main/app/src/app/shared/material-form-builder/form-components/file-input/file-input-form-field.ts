/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { AbstractFileFormField } from "../../model/abstract-file-form-field";
import { FieldType } from "../../model/field-type.enum";

export class FileInputFormField<
  T extends File = File,
> extends AbstractFileFormField<T> {
  fieldType: FieldType = FieldType.FILE_INPUT;

  constructor() {
    super();
  }
}
