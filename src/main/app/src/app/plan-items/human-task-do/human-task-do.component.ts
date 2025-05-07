/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, EventEmitter, Input, OnInit, Output } from "@angular/core";
import { FormGroup } from "@angular/forms";
import { MatDrawer } from "@angular/material/sidenav";
import { ActivatedRoute } from "@angular/router";
import { AbstractTaakFormulier } from "../../formulieren/taken/abstract-taak-formulier";
import { TaakFormulierenService } from "../../formulieren/taken/taak-formulieren.service";
import { AbstractFormField } from "../../shared/material-form-builder/model/abstract-form-field";
import { FormConfig } from "../../shared/material-form-builder/model/form-config";
import { FormConfigBuilder } from "../../shared/material-form-builder/model/form-config-builder";
import { GeneratedType } from "../../shared/utils/generated-types";
import { PlanItem } from "../model/plan-item";
import { PlanItemType } from "../model/plan-item-type.enum";
import { PlanItemsService } from "../plan-items.service";

@Component({
  selector: "zac-human-task-do",
  templateUrl: "./human-task-do.component.html",
  styleUrls: ["./human-task-do.component.less"],
})
export class HumanTaskDoComponent implements OnInit {
  formItems: Array<AbstractFormField[]>;
  formConfig: FormConfig;
  private formulier: AbstractTaakFormulier;
  @Input() planItem: PlanItem;
  @Input() sideNav: MatDrawer;
  @Input() zaak: GeneratedType<"RestZaak">;
  @Output() done = new EventEmitter<void>();

  constructor(
    private route: ActivatedRoute,
    private planItemsService: PlanItemsService,
    private taakFormulierenService: TaakFormulierenService,
  ) {}

  ngOnInit(): void {
    this.formConfig = new FormConfigBuilder()
      .saveText("actie.starten")
      .cancelText("actie.annuleren")
      .build();

    if (this.planItem.type === PlanItemType.HumanTask) {
      this.formulier = this.taakFormulierenService
        .getFormulierBuilder(this.planItem.formulierDefinitie)
        .startForm(this.planItem, this.zaak)
        .build();
      if (this.formulier.disablePartialSave) {
        this.formConfig.partialButtonText = null;
      }
      this.formItems = this.formulier.form;
    } else {
      this.formItems = [[]];
    }
  }

  onFormSubmit(formGroup: FormGroup): void {
    if (formGroup) {
      this.planItemsService
        .doHumanTaskPlanItem(this.formulier.getHumanTaskData(formGroup))
        .subscribe(() => {
          this.done.emit();
        });
    } else {
      // cancel button clicked
      this.done.emit();
    }
  }
}
