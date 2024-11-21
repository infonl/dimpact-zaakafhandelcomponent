/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 *
 */

import { Component, Input, OnInit, EventEmitter, Output } from "@angular/core";
import { FormControl } from "@angular/forms";
import { TranslateService } from "@ngx-translate/core";
import { GeneratedType } from "../../../../shared/utils/generated-types";
// import { EventEmitter } from "stream";

@Component({
  selector: "smart-documents-form-item",
  templateUrl: "./smart-documents-form-item.component.html",
  styleUrls: ["./smart-documents-form-item.component.less"],
})
export class SmartDocumentsFormItemComponent implements OnInit {
  @Input() node: any;
  @Input() informationObjectTypes: GeneratedType<"RestInformatieobjecttype">[];
  @Output() selectionChange = new EventEmitter<
    GeneratedType<"RestMappedSmartDocumentsTemplate">
  >();

  confidentiality = new FormControl({ value: "", disabled: true });
  checkbox = new FormControl({ value: false, disabled: false });

  previousInformatieObjectTypeUUID: string;

  constructor(private readonly translateService: TranslateService) {}

  ngOnInit() {
    this.previousInformatieObjectTypeUUID = this.node?.informatieObjectTypeUUID;
    this.updateFormControls();
  }

  clearSelectedDocumentType() {
    this.node.informatieObjectTypeUUID = "";
    this.updateFormControls();
  }

  updateFormControls() {
    const { informatieObjectTypeUUID } = this.node;

    const confidentiality = this.informationObjectTypes.find(
      ({ uuid }) => informatieObjectTypeUUID === uuid,
    )?.vertrouwelijkheidaanduiding;

    this.confidentiality.setValue(
      confidentiality
        ? this.translateService.instant(
            `vertrouwelijkheidaanduiding.${confidentiality}`,
          )
        : null,
    );

    this.updateCheckbox(
      informatieObjectTypeUUID && informatieObjectTypeUUID !== "",
    );

    if (
      this.node.informatieObjectTypeUUID !==
      this.previousInformatieObjectTypeUUID
    ) {
      this.selectionChange.emit({ ...this.node });
      this.previousInformatieObjectTypeUUID =
        this.node.informatieObjectTypeUUID;
    }
  }

  private updateCheckbox(hasValue: boolean) {
    this.checkbox[hasValue ? "enable" : "disable"]();
    this.checkbox.setValue(hasValue);
  }
}
