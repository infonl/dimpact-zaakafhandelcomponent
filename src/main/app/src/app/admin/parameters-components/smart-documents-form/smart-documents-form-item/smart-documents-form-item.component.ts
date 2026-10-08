/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 *
 */

import { NgFor } from "@angular/common";
import { Component, input, OnInit, output } from "@angular/core";
import { FormControl, ReactiveFormsModule } from "@angular/forms";
import { MatCheckboxModule } from "@angular/material/checkbox";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatInputModule } from "@angular/material/input";
import { MatSelectModule } from "@angular/material/select";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { I18nKeyPipe } from "../../../../shared/pipes/i18n-key.pipe";
import { GeneratedType } from "../../../../shared/utils/generated-types";
import { toI18nKey } from "../../../../shared/utils/i18n-key";

@Component({
  selector: "smart-documents-form-item",
  templateUrl: "./smart-documents-form-item.component.html",
  styleUrls: ["./smart-documents-form-item.component.less"],
  standalone: true,
  imports: [
    I18nKeyPipe,
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
  readonly node =
    input.required<GeneratedType<"RestMappedSmartDocumentsTemplate">>();
  readonly informationObjectTypes =
    input.required<GeneratedType<"RestInformatieobjecttype">[]>();
  readonly selectionChange =
    output<GeneratedType<"RestMappedSmartDocumentsTemplate">>();

  protected confidentiality = new FormControl({ value: "", disabled: true });
  protected checkbox = new FormControl({ value: false, disabled: false });

  private previousInformatieObjectTypeUUID: string | undefined = undefined;

  constructor(private readonly translateService: TranslateService) {}

  ngOnInit() {
    this.previousInformatieObjectTypeUUID =
      this.node().informatieObjectTypeUUID;
    this.updateFormControls();
  }

  protected clearSelectedDocumentType() {
    this.selectDocumentType("");
  }

  protected selectDocumentType(informatieObjectTypeUUID: string) {
    this.node().informatieObjectTypeUUID = informatieObjectTypeUUID;
    this.updateFormControls();
  }

  updateFormControls() {
    const node = this.node();
    const { informatieObjectTypeUUID } = node;

    const confidentiality = this.informationObjectTypes().find(
      ({ uuid }) => informatieObjectTypeUUID === uuid,
    )?.vertrouwelijkheidaanduiding;

    this.confidentiality.setValue(
      confidentiality
        ? this.translateService.instant(
            toI18nKey(`vertrouwelijkheidaanduiding.${confidentiality}`),
          )
        : null,
    );

    this.updateCheckbox(
      Boolean(informatieObjectTypeUUID && informatieObjectTypeUUID !== ""),
    );

    if (informatieObjectTypeUUID !== this.previousInformatieObjectTypeUUID) {
      this.selectionChange.emit({ ...node });
      this.previousInformatieObjectTypeUUID = informatieObjectTypeUUID;
    }
  }

  private updateCheckbox(hasValue: boolean) {
    this.checkbox[hasValue ? "enable" : "disable"]();
    this.checkbox.setValue(hasValue);
  }
}
