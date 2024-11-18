/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 *
 */

import { Component, Input, OnInit } from "@angular/core";
import { FormControl } from "@angular/forms";
import { TranslateService } from "@ngx-translate/core";
import { GeneratedType } from "../../../../shared/utils/generated-types";

@Component({
  selector: "smart-documents-form-item",
  templateUrl: "./smart-documents-form-item.component.html",
})
export class SmartDocumentsFormItemComponent implements OnInit {
  @Input() template: GeneratedType<"RestMappedSmartDocumentsTemplate">;
  @Input() informationObjectTypes: GeneratedType<"RestInformatieobjecttype">[];

  confidentiality = new FormControl({ value: "", disabled: true });
  enabled = new FormControl({ value: false, disabled: false });

  constructor(private readonly translateService: TranslateService) {}

  ngOnInit() {
    this.updateEnabledStatus();
  }

  clearSelectedDocumentType() {
    this.template.informatieObjectTypeUUID = "";
    this.updateFormControls();
  }

  updateFormControls() {
    const { informatieObjectTypeUUID } = this.template;

    const confidentiality = this.informationObjectTypes.find(
      ({ uuid }) => informatieObjectTypeUUID === uuid,
    )?.vertrouwelijkheidaanduiding;

    const translated = this.translateService.instant(
      `vertrouwelijkheidaanduiding.${confidentiality}`,
    );

    this.confidentiality.setValue(translated);
    this.updateEnabledStatus();
  }

  private updateEnabledStatus() {
    this.enabled.setValue(this.template.informatieObjectTypeUUID !== "");
    if (this.template.informatieObjectTypeUUID !== "") {
      this.enabled.enable();
    } else {
      this.enabled.disable();
    }
  }
}
