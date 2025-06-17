/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, EventEmitter, Input, OnInit, Output } from "@angular/core";
import { FormGroup } from "@angular/forms";
import { AbstractProcessFormulier } from "../../formulieren/process/abstract-process-formulier";
import { ProcessFormulierenService } from "../../formulieren/process/process-formulieren.service";
import { AbstractFormField } from "../../shared/material-form-builder/model/abstract-form-field";
import { FormConfig } from "../../shared/material-form-builder/model/form-config";
import { FormConfigBuilder } from "../../shared/material-form-builder/model/form-config-builder";
import { GeneratedType } from "../../shared/utils/generated-types";
import { PlanItemsService } from "../plan-items.service";

@Component({
  selector: "zac-process-task-do",
  templateUrl: "./process-task-do.component.html",
  styleUrls: ["./process-task-do.component.less"],
})
export class ProcessTaskDoComponent implements OnInit {
  formItems: Array<AbstractFormField[]>;
  formConfig: FormConfig;
  private formulier: AbstractProcessFormulier;
  @Input() planItem: GeneratedType<"RESTPlanItem">;
  @Input() zaak: GeneratedType<"RestZaak">;
  @Output() done = new EventEmitter<void>();

  constructor(
    private planItemsService: PlanItemsService,
    private processFormulierenService: ProcessFormulierenService,
  ) {}

  ngOnInit(): void {
    this.formConfig = new FormConfigBuilder()
      .saveText("actie.starten")
      .cancelText("actie.annuleren")
      .build();
    this.formulier = this.processFormulierenService
      .getFormulierBuilder()
      .form()
      .build();
  }

  onFormSubmit(formGroup?: FormGroup) {
    if (!formGroup) {
      this.done.emit();
      return;
    }

    this.planItemsService
      .doProcessTaskPlanItem({
        ...this.formulier.getData(formGroup),
        planItemInstanceId: this.planItem.id,
      })
      .subscribe(() => {
        this.done.emit();
      });
  }
}
