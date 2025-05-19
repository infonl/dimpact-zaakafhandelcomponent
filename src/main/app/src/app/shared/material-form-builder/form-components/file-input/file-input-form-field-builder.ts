/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { AbstractFileFormFieldBuilder } from "../../model/abstract-file-form-field-builder";
import { FileInputFormField } from "./file-input-form-field";

export class FileInputFormFieldBuilder<T extends File = File> extends AbstractFileFormFieldBuilder<T> {
  readonly formField: FileInputFormField<T>;

  constructor(value?: T) {
    super();
    this.formField = new FileInputFormField();
    this.formField.initControl(value);
  }
}
