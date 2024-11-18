/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 *
 */

import { Component, Input } from "@angular/core";
import { GeneratedType } from "../../../../shared/utils/generated-types";
import {AbstractControl, FormGroup} from "@angular/forms";

@Component({
  selector: "smart-documents-form-item",
  templateUrl: "./smart-documents-form-item.component.html",
})
export class SmartDocumentsFormItemComponent {
  @Input() template: GeneratedType<"RestSmartDocumentsTemplate">;
  @Input() documentTypes: GeneratedType<"RestMappedSmartDocumentsTemplate">[];
  @Input() formGroup: FormGroup<{ documentType: AbstractControl<GeneratedType<"RestMappedSmartDocumentsTemplate">, GeneratedType<"RestMappedSmartDocumentsTemplate">> }>;
  @Input() initialInformationObjectTypeUuid?: string;

  documentType: GeneratedType<"RestMappedSmartDocumentsTemplate">;

  constructor() {
    if (!this.initialInformationObjectTypeUuid) return;

    this.documentType = this.documentTypes.find(
      ({ informatieObjectTypeUUID }) =>
        (this.initialInformationObjectTypeUuid = informatieObjectTypeUUID),
    );
    this.formGroup.setValue({ documentType: this.documentType });
  }

  clearSelectedDocumentType() {
    console.log("clearSelectedDocumentType");
  }
}
