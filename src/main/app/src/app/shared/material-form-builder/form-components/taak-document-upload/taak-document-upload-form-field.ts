/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { AbstractFileFormField } from "../../model/abstract-file-form-field";
import { FieldType } from "../../model/field-type.enum";

export class TaakDocumentUploadFormField<T extends File = File> extends AbstractFileFormField<T> {
  fieldType: FieldType = FieldType.TAAK_DOCUMENT_UPLOAD;

  defaultTitel: string;
  zaakUUID: string;

  constructor() {
    super();
  }
}
