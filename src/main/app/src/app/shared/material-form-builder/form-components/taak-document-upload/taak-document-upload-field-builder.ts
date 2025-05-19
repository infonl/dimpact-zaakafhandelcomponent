/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { AbstractFileFormFieldBuilder } from "../../model/abstract-file-form-field-builder";
import { TaakDocumentUploadFormField } from "./taak-document-upload-form-field";

export class TaakDocumentUploadFieldBuilder<T extends File = File> extends AbstractFileFormFieldBuilder<T> {
  readonly formField: TaakDocumentUploadFormField<T>;

  constructor(value?: T) {
    super();
    this.formField = new TaakDocumentUploadFormField();
    this.formField.initControl(value);
  }

  defaultTitel(titel: string): this {
    this.formField.defaultTitel = titel;
    return this;
  }

  zaakUUID(zaakUUID: string): this {
    this.formField.zaakUUID = zaakUUID;
    return this;
  }

  validate(): void {
    super.validate();
    if (!this.formField.zaakUUID) {
      throw new Error("zaakUUID is required");
    }
  }
}
