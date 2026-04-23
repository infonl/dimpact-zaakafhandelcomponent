/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 *
 */

import { NgFor } from "@angular/common";
import { Component, EventEmitter, Input, OnInit, Output } from "@angular/core";
import { FormControl, ReactiveFormsModule } from "@angular/forms";
import { MatCheckboxModule } from "@angular/material/checkbox";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatInputModule } from "@angular/material/input";
import { MatSelectModule } from "@angular/material/select";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { GeneratedType } from "../../../../shared/utils/generated-types";

@Component({
  selector: "smart-documents-form-item",
  templateUrl: "./smart-documents-form-item.component.html",
  styleUrls: ["./smart-documents-form-item.component.less"],
  standalone: true,
  imports: [
    NgFor,
    ReactiveFormsModule,
    MatCheckboxModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    TranslateModule,
  ],
})
export class SmartDocumentsFormItemComponent implements OnInit {
  @Input({ required: true })
  node!: GeneratedType<"RestMappedSmartDocumentsTemplate">;
  @Input({ required: true })
  informationObjectTypes!: GeneratedType<"RestInformatieobjecttype">[];
  @Output() selectionChange = new EventEmitter<
    GeneratedType<"RestMappedSmartDocumentsTemplate">
  >();

  protected confidentiality = new FormControl({ value: "", disabled: true });
  protected checkbox = new FormControl({ value: false, disabled: false });

  private previousInformatieObjectTypeUUID: string | undefined = undefined;

  constructor(private readonly translateService: TranslateService) {}

  ngOnInit() {
    this.previousInformatieObjectTypeUUID = this.node?.informatieObjectTypeUUID;
    this.updateFormControls();
  }

  protected clearSelectedDocumentType() {
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
      Boolean(informatieObjectTypeUUID && informatieObjectTypeUUID !== ""),
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
