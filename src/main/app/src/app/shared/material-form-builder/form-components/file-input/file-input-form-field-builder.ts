/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { AbstractFileFormFieldBuilder } from "../../model/abstract-file-form-field-builder";
import { FileInputFormField } from "./file-input-form-field";

export class FileInputFormFieldBuilder extends AbstractFileFormFieldBuilder {
  readonly formField: FileInputFormField;

  constructor(value?: any) {
    super();
    this.formField = new FileInputFormField();
    this.formField.initControl(value);
  }
}
