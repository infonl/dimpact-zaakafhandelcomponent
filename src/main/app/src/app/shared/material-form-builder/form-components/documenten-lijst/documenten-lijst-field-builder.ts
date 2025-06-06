/*
 * SPDX-FileCopyrightText: 2023 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Observable } from "rxjs";
import { GeneratedType } from "../../../utils/generated-types";
import { AbstractFormFieldBuilder } from "../../model/abstract-form-field-builder";
import { DocumentenLijstFormField } from "./documenten-lijst-form-field";

export class DocumentenLijstFieldBuilder<
  T extends string = string,
> extends AbstractFormFieldBuilder<T> {
  readonly formField: DocumentenLijstFormField<T>;

  constructor() {
    super();
    this.formField = new DocumentenLijstFormField();
    this.formField.initControl();
  }

  documenten(
    documenten: Observable<GeneratedType<"RestEnkelvoudigInformatieobject">[]>,
  ): this {
    this.formField.documenten = documenten;
    return this;
  }

  documentenChecked(documentUUIDs: string[]): this {
    this.formField.documentenChecked = documentUUIDs;
    return this;
  }

  removeColumn(
    column:
      | "select"
      | "titel"
      | "documentType"
      | "status"
      | "versie"
      | "auteur"
      | "creatiedatum"
      | "bestandsomvang"
      | "indicaties"
      | "url",
  ): this {
    this.formField.removeColumn(column);
    return this;
  }

  openInNieuweTab() {
    this.formField.openInNieuweTab = true;
    return this;
  }

  selectLabel(label: string): this {
    this.formField.selectLabel = label;
    return this;
  }
}
