/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { AbstractFileFormFieldBuilder } from "../../model/abstract-file-form-field-builder";
import { FileFormField } from "./file-form-field";

export class FileFormFieldBuilder<T extends File = File> extends AbstractFileFormFieldBuilder<T> {
  readonly formField: FileFormField<T>;

  uploadURL(url: string): this {
    this.formField.uploadURL = url;
    return this;
  }

  validate(): void {
    super.validate();
    if (!this.formField.uploadURL) {
      throw new Error("Missing value for restURL");
    }
  }

  constructor(value?: T) {
    super();
    this.formField = new FileFormField();
    this.formField.initControl(value);
  }
}
