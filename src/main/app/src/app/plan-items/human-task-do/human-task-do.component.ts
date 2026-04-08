/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  Component,
  DestroyRef,
  EventEmitter,
  Input,
  OnInit,
  Output,
  inject,
} from "@angular/core";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { FormBuilder, FormGroup, Validators } from "@angular/forms";
import { MatDrawer } from "@angular/material/sidenav";
import { injectMutation } from "@tanstack/angular-query-experimental";
import { EMPTY, lastValueFrom, switchMap } from "rxjs";
import { FoutAfhandelingService } from "src/app/fout-afhandeling/fout-afhandeling.service";
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
  standalone: false,
})
export class HumanTaskDoComponent implements OnInit {
  private readonly destroyRef = inject(DestroyRef);
  private formulier?: AbstractTaakFormulier;
  @Input() planItem?: GeneratedType<"RESTPlanItem"> | null = null;
  @Input({ required: true }) sideNav!: MatDrawer;
  @Input({ required: true }) zaak!: GeneratedType<"RestZaak">;
  @Output() done = new EventEmitter<void>();

  protected readonly doHumanTaskPlanItemMutation = injectMutation(() => ({
    ...this.planItemsService.doHumanTaskPlanItem(),
    onSuccess: () => {
      this.done.emit();
    },
    onError: (error) => {
      this.foutAfhandelingService.foutAfhandelen(error);
    },
  }));

  protected formItems: Array<AbstractFormField[]> = [];
  protected formConfig = new FormConfigBuilder()
    .saveText("actie.starten")
    .cancelText("actie.annuleren")
    .build();

  protected form = this.formBuilder.group({});
  protected formFields: FormField[] = [];

  protected _formConfig: NewFormConfig = {
    submitLabel: "actie.starten",
  };

  constructor(
    private readonly planItemsService: PlanItemsService,
    private readonly identityService: IdentityService,
    private readonly foutAfhandelingService: FoutAfhandelingService,
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
          this.planItem,
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

      const groups = await lastValueFrom(
        this.identityService.listBehandelaarGroupsForZaaktype(
          this.zaak.zaaktype.uuid,
        ),
      );

      if (this.planItem.groepId) {
        const defaultGroup = groups.find(
          (group) => group.id === this.planItem!.groepId,
        );
        if (defaultGroup) {
          groupControl.setValue(defaultGroup);
        }
      }

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

      if (groupControl.value) {
        userControl.enable();
        this.identityService
          .listUsersInGroup(groupControl.value.id)
          .pipe(takeUntilDestroyed(this.destroyRef))
          .subscribe((users) => this.updateUserOptions(users));
      }
      groupControl.valueChanges
        .pipe(
          takeUntilDestroyed(this.destroyRef),
          switchMap((value) => {
            userControl.reset();
            if (!value) {
              userControl.disable();
              return EMPTY;
            }
            userControl.enable();
            return this.identityService.listUsersInGroup(value.id);
          }),
        )
        .subscribe((users) => this.updateUserOptions(users));
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

  private updateUserOptions(users: GeneratedType<"RestUser">[]) {
    this.formFields = this.formFields.map((field) => {
      if (field.type === "auto-complete" && field.key === "user")
        field.options = users;
      return field;
    });
  }

  onFormCancel() {
    this.done.emit();
  }

  onFormSubmit(formGroup?: FormGroup) {
    if (!formGroup) {
      this.done.emit();
      return;
    }
    try {
      if (!this.formulier) throw new Error("Handling form in Angular way");
      const taakData = this.formulier.getHumanTaskData(formGroup);
      this.doHumanTaskPlanItemMutation.mutate(taakData);
    } catch {
      // Handling form in Angular way
      this.doHumanTaskPlanItemMutation.mutate({
        planItemInstanceId: this.planItem!.id!,
        groep: this.form.get("group")!.value!,
        medewerker: this.form.get("user")!.value!,
        fataledatum: this.form.get("taakFataledatum")?.value,
        taakStuurGegevens: {
          sendMail: this.form.get("taakStuurGegevens.sendMail")?.value ?? false,
          mail: this.form.get("taakStuurGegevens.mail")?.value,
        },
        taakdata: mapFormGroupToTaskData(formGroup, {
          ignoreKeys: ["group", "user"],
        }),
      });
    }
  }
}
