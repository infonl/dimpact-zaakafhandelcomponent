/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 *
 */

import { Component, Input } from "@angular/core";
import { FormGroup } from "@angular/forms";
import { GeneratedType } from "../../../../shared/utils/generated-types";

@Component({
  selector: "smart-documents-form-item",
  templateUrl: "./smart-documents-form-item.component.html",
})
export class SmartDocumentsFormItemComponent {
  @Input() template: GeneratedType<"RestMappedSmartDocumentsTemplate">;
  @Input() documentTypes: GeneratedType<"RestInformatieobjecttype">[];
  @Input() informationObjectTypes: GeneratedType<"RestInformatieobjecttype">[];
  @Input() formGroup: FormGroup;

  confidentiality = ''

  constructor() {

  }

  clearSelectedDocumentType() {
    console.log("clearSelectedDocumentType");
    this.template.informatieObjectTypeUUID = ''
  }
}
