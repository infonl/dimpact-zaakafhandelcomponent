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
  styleUrls: ["./smart-documents-form-item.component.less"],
})
export class SmartDocumentsFormItemComponent implements OnInit {
  @Input() template: GeneratedType<"RestMappedSmartDocumentsTemplate">;
  @Input() informationObjectTypes: GeneratedType<"RestInformatieobjecttype">[];

  confidentiality = new FormControl({ value: "", disabled: true });
  checkbox = new FormControl({ value: false, disabled: false });

  constructor(private readonly translateService: TranslateService) {}

  ngOnInit() {
    this.updateFormControls();
  }

  clearSelectedDocumentType() {
    this.template.informatieObjectTypeUUID = undefined;
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

    if(confidentiality) {
      this.confidentiality.setValue(translated);
    }

    this.updateEnabledStatus();
  }

  private updateEnabledStatus() {
    const hasValue = this.template.informatieObjectTypeUUID && this.template.informatieObjectTypeUUID !== ""

    if (hasValue) {
      this.checkbox.enable();
    } else {
      this.checkbox.disable();
    }

    this.checkbox.setValue(hasValue);
  }
}
