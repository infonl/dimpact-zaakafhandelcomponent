/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NgIf } from "@angular/common";
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
import {
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from "@angular/forms";
import { MatIconButton } from "@angular/material/button";
import { MatDivider } from "@angular/material/divider";
import { MatIcon } from "@angular/material/icon";
import { MatProgressSpinner } from "@angular/material/progress-spinner";
import { MatDrawer } from "@angular/material/sidenav";
import { MatToolbar } from "@angular/material/toolbar";
import { TranslatePipe } from "@ngx-translate/core";
import { injectMutation } from "@tanstack/angular-query-experimental";
import { EMPTY, lastValueFrom, switchMap } from "rxjs";
import { FoutAfhandelingService } from "src/app/fout-afhandeling/fout-afhandeling.service";
import { TaakFormulierenService } from "../../formulieren/taken/taak-formulieren.service";
import { mapFormGroupToTaskData } from "../../formulieren/taken/taak.utils";
import { IdentityService } from "../../identity/identity.service";
import { ZacComposedForm } from "../../shared/form/composed-form/composed-form.component";
import {
  FormField,
  FormConfig as NewFormConfig,
} from "../../shared/form/composed-form/form-field.types";
import { GeneratedType } from "../../shared/utils/generated-types";
import { PlanItemsService } from "../plan-items.service";

@Component({
  selector: "zac-human-task-do",
  templateUrl: "./human-task-do.component.html",
  styleUrls: ["./human-task-do.component.less"],
  standalone: true,
  imports: [
    MatDivider,
    MatIcon,
    MatIconButton,
    MatProgressSpinner,
    MatToolbar,
    NgIf,
    ReactiveFormsModule,
    TranslatePipe,
    ZacComposedForm,
  ],
})
export class HumanTaskDoComponent implements OnInit {
  private readonly destroyRef = inject(DestroyRef);
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
      this.formFields = [];
      return;
    }

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
        this.zaak.zaaktype.omschrijving!,
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
  }

  private updateUserOptions(users: GeneratedType<"RestUser">[]) {
    this.formFields = this.formFields.map((field) => {
      if (field.type === "auto-complete" && field.key === "user")
        field.options = users;
      return field;
    });
  }

  protected onFormCancel() {
    this.done.emit();
  }

  protected onFormSubmit(formGroup?: FormGroup) {
    if (!formGroup) {
      this.done.emit();
      return;
    }
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
