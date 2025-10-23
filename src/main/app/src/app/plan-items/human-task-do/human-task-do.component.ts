/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, EventEmitter, Input, OnInit, Output } from "@angular/core";
import { FormBuilder, FormGroup, Validators } from "@angular/forms";
import { MatDrawer } from "@angular/material/sidenav";
import { lastValueFrom } from "rxjs";
import { AbstractTaakFormulier } from "../../formulieren/taken/abstract-taak-formulier";
import { TaakFormulierenService } from "../../formulieren/taken/taak-formulieren.service";
import { mapFormGroupToTaskData } from "../../formulieren/taken/taak.utils";
import { IdentityService } from "../../identity/identity.service";
import { FormField, FormConfig as NewFormConfig } from "../../shared/form/form";
import { AbstractFormField } from "../../shared/material-form-builder/model/abstract-form-field";
import { FormConfigBuilder } from "../../shared/material-form-builder/model/form-config-builder";
import { GeneratedType } from "../../shared/utils/generated-types";
import { PlanItemsService } from "../plan-items.service";

@Component({
  selector: "zac-human-task-do",
  templateUrl: "./human-task-do.component.html",
  styleUrls: ["./human-task-do.component.less"],
})
export class HumanTaskDoComponent implements OnInit {
  private formulier?: AbstractTaakFormulier;
  @Input() planItem?: GeneratedType<"RESTPlanItem"> | null = null;
  @Input({ required: true }) sideNav!: MatDrawer;
  @Input({ required: true }) zaak!: GeneratedType<"RestZaak">;
  @Output() done = new EventEmitter<void>();

  protected formItems: Array<AbstractFormField[]> = [];
  protected formConfig = new FormConfigBuilder()
    .saveText("actie.starten")
    .cancelText("actie.annuleren")
    .build();

  protected form = this.formBuilder.group({});
  protected formFields: FormField[] = [];
  protected loading = false;

  protected _formConfig: NewFormConfig = {
    submitLabel: "actie.starten",
  };

  constructor(
    private readonly planItemsService: PlanItemsService,
    private readonly identityService: IdentityService,
    private readonly taakFormulierenService: TaakFormulierenService,
    private readonly formBuilder: FormBuilder,
  ) {}

  async ngOnInit() {
    if (this.planItem?.type !== "HUMAN_TASK") {
      this.formItems = [[]];
      this.formFields = [];
      return;
    }

    try {
      const formFields =
        await this.taakFormulierenService.getAngularRequestFormBuilder(
          this.zaak,
          this.planItem.formulierDefinitie,
        );

      formFields.map((formField) => {
        this.form.addControl(
          formField.key,
          formField.control ?? this.formBuilder.control(null),
        );

        this.formFields.push(formField);
      });

      this.formFields.push({
        type: "plain-text",
        key: "actie.taak.toewijzing",
        label: "actie.taak.toewijzing",
      });

      const groupControl =
        this.formBuilder.control<GeneratedType<"RestGroup"> | null>(null, [
          Validators.required,
        ]);
      this.form.addControl("group", groupControl);

      const groups = await lastValueFrom(this.identityService.listGroups());
      this.formFields.push({
        type: "auto-complete",
        key: "group",
        label: "actie.taak.toekennen.groep",
        options: groups,
        optionDisplayValue: "naam",
      });

      const userControl =
        this.formBuilder.control<GeneratedType<"RestUser"> | null>(null, []);
      this.form.addControl("user", userControl);
      userControl.disable();
      this.formFields.push({
        type: "auto-complete",
        key: "user",
        label: "actie.taak.toekennen.medewerker",
        options: [],
        optionDisplayValue: "naam",
      });

      groupControl.valueChanges.subscribe((value) => {
        userControl.reset();

        if (!value) {
          userControl.disable();
          return;
        }

        userControl.enable();
        this.identityService.listUsersInGroup(value.id).subscribe((users) => {
          this.formFields = this.formFields.map((field) => {
            if (field.type === "auto-complete" && field.key === "user")
              field.options = users;
            return field;
          });
        });
      });
    } catch (e) {
      console.warn(e);

      this.formulier = this.taakFormulierenService
        .getFormulierBuilder(this.planItem.formulierDefinitie)
        .startForm(this.planItem, this.zaak)
        .build();
      if (this.formulier.disablePartialSave) {
        this.formConfig.partialButtonText = null;
      }
      this.formItems = this.formulier.form;
    }
  }

  onFormCancel() {
    this.done.emit();
  }

  onFormSubmit(formGroup?: FormGroup) {
    if (!formGroup) {
      this.done.emit();
      return;
    }
    this.loading = true;

    try {
      if (!this.formulier) throw new Error("Handling form in Angular way");
      const taakData = this.formulier.getHumanTaskData(formGroup);
      this.planItemsService.doHumanTaskPlanItem(taakData).subscribe(() => {
        this.done.emit();
      });
    } catch {
      // Handling form in Angular way
      this.planItemsService
        .doHumanTaskPlanItem({
          planItemInstanceId: this.planItem!.id!,
          groep: this.form.get("group")!.value!,
          medewerker: this.form.get("user")!.value!,
          taakStuurGegevens: {
            sendMail:
              this.form.get("taakStuurGegevens.sendMail")?.value ?? false,
            mail: this.form.get("taakStuurGegevens.mail")?.value,
          },
          taakdata: mapFormGroupToTaskData(formGroup, {
            ignoreKeys: ["group", "user"],
          }),
        })
        .subscribe({
          next: () => {
            this.done.emit();
            this.loading = false;
          },
          error: () => {
            this.loading = false;
          },
        });
    }
  }
}
