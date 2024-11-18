/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 *
 */

import { Component, Input } from "@angular/core";
import { FormControl, FormGroup } from "@angular/forms";
import { TranslateService } from "@ngx-translate/core";
import { GeneratedType } from "../../../../shared/utils/generated-types";

@Component({
  selector: "smart-documents-form-item",
  templateUrl: "./smart-documents-form-item.component.html",
})
export class SmartDocumentsFormItemComponent {
  @Input() template: GeneratedType<"RestMappedSmartDocumentsTemplate">;
  @Input() informationObjectTypes: GeneratedType<"RestInformatieobjecttype">[];
  @Input() formGroup: FormGroup;

  confidentiality = new FormControl({ value: "", disabled: true });
  enabled = new FormControl({ value: false, disabled: false });

  constructor(private readonly translateService: TranslateService) {}

  ngOnInit() {
    this.enabled.setValue(this.template.informatieObjectTypeUUID !== "");
  }

  clearSelectedDocumentType() {
    this.template.informatieObjectTypeUUID = "";
    this.confidentiality.reset();
    this.enabled.reset();
  }

  setConfidentiality() {
    const { informatieObjectTypeUUID } = this.template;

    const confidentiality = this.informationObjectTypes.find(
      ({ uuid }) => informatieObjectTypeUUID === uuid,
    )?.vertrouwelijkheidaanduiding;

    const translated = this.translateService.instant(
      `vertrouwelijkheidaanduiding.${confidentiality}`,
    );

    this.confidentiality.setValue(translated);
  }
}
