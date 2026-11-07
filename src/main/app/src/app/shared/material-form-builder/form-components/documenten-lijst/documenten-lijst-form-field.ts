/*
 * SPDX-FileCopyrightText: 2023 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Observable } from "rxjs";
import { GeneratedType } from "../../../utils/generated-types";
import { AbstractFormControlField } from "../../model/abstract-form-control-field";
import { FieldType } from "../../model/field-type.enum";

export class DocumentenLijstFormField extends AbstractFormControlField {
  fieldType = FieldType.DOCUMENTEN_LIJST;
  documenten: Observable<GeneratedType<"RestEnkelvoudigInformatieobject">[]>;
  documentenChecked: string[];
  columns: string[] = [
    "select",
    "titel",
    "documentType",
    "status",
    "versie",
    "auteur",
    "creatiedatum",
    "bestandsomvang",
    "indicaties",
    "url",
  ];
  selectLabel = "";
  openInNieuweTab = false;

  constructor() {
    super();
  }

  hasReadonlyView() {
    return true;
  }

  updateDocumenten(
    documenten: Observable<GeneratedType<"RestEnkelvoudigInformatieobject">[]>,
  ) {
    this.documenten = documenten;
  }

  removeColumn(id: string) {
    this.columns.splice(this.columns.indexOf(id), 1);
  }
}
