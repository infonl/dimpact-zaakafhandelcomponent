/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, EventEmitter, Input, OnInit, Output } from "@angular/core";
import { FormGroup } from "@angular/forms";
import { AbstractProcessFormulier } from "../../formulieren/process/abstract-process-formulier";
import { ProcessFormulierenService } from "../../formulieren/process/process-formulieren.service";
import { AbstractFormField } from "../../shared/material-form-builder/model/abstract-form-field";
import { FormConfig } from "../../shared/material-form-builder/model/form-config";
import { FormConfigBuilder } from "../../shared/material-form-builder/model/form-config-builder";
import { Zaak } from "../../zaken/model/zaak";
import { PlanItem } from "../model/plan-item";
import { ProcessTaskData } from "../model/process-task-data";
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
  @Input() planItem: PlanItem;
  @Input() zaak: Zaak;
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
      .getFormulierBuilder(this.planItem.formulierDefinitie)
      .form(this.planItem, this.zaak)
      .build();
  }

  onFormSubmit(formGroup: FormGroup): void {
    if (formGroup) {
      const processTaskData: ProcessTaskData =
        this.formulier.getData(formGroup);
      processTaskData.planItemInstanceId = this.planItem.id;
      this.planItemsService
        .doProcessTaskPlanItem(processTaskData)
        .subscribe(() => {
          this.done.emit();
        });
    } else {
      // cancel button clicked
      this.done.emit();
    }
  }
}
