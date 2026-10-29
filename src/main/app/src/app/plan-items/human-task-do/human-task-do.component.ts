/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HttpErrorResponse } from "@angular/common/http";
import {
  Component,
  DestroyRef,
  effect,
  inject,
  input,
  OnInit,
  output,
  signal,
  ChangeDetectionStrategy,
} from "@angular/core";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { FormBuilder, FormGroup, Validators } from "@angular/forms";
import { MatIconButton } from "@angular/material/button";
import { MatDivider } from "@angular/material/divider";
import { MatIcon } from "@angular/material/icon";
import { MatProgressSpinner } from "@angular/material/progress-spinner";
import { MatDrawer } from "@angular/material/sidenav";
import { MatToolbar } from "@angular/material/toolbar";
import { TranslatePipe } from "@ngx-translate/core";
import { injectQuery, QueryClient } from "@tanstack/angular-query-experimental";
import { FoutAfhandelingService } from "src/app/fout-afhandeling/fout-afhandeling.service";
import { TaakFormulierenService } from "../../formulieren/taken/taak-formulieren.service";
import { mapFormGroupToTaskData } from "../../formulieren/taken/taak.utils";
import { IdentityService } from "../../identity/identity.service";
import { ZacComposedForm } from "../../shared/form/composed-form/composed-form.component";
import {
  FormConfig,
  FormField,
} from "../../shared/form/composed-form/form-field.types";
import { injectMutation } from "../../shared/http/inject-mutation";
import { GeneratedType } from "../../shared/utils/generated-types";
import { PlanItemsService } from "../plan-items.service";

@Component({
  selector: "zac-human-task-do",
  templateUrl: "./human-task-do.component.html",
  styleUrls: ["./human-task-do.component.less"],
  standalone: true,
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [
    MatDivider,
    MatIcon,
    MatIconButton,
    MatProgressSpinner,
    MatToolbar,
    TranslatePipe,
    ZacComposedForm,
  ],
})
export class HumanTaskDoComponent implements OnInit {
  private readonly destroyRef = inject(DestroyRef);
  private readonly planItemsService = inject(PlanItemsService);
  private readonly identityService = inject(IdentityService);
  private readonly foutAfhandelingService = inject(FoutAfhandelingService);
  private readonly taakFormulierenService = inject(TaakFormulierenService);
  private readonly formBuilder = inject(FormBuilder);
  private readonly queryClient = inject(QueryClient);

  readonly planItem = input<GeneratedType<"RestPlanItem"> | null | undefined>(
    null,
  );
  readonly sideNav = input.required<MatDrawer>();
  readonly zaak = input.required<GeneratedType<"RestZaak">>();
  readonly done = output<void>();

  private readonly selectedGroupId = signal<string | undefined>(undefined);

  private readonly usersQuery = injectQuery(() => ({
    ...this.identityService.listUsersInGroup(this.selectedGroupId() ?? ""),
    enabled: !!this.selectedGroupId(),
  }));

  protected readonly doHumanTaskPlanItemMutation = injectMutation(
    () => this.planItemsService.doHumanTaskPlanItem(this.zaak().uuid),
    { onSuccess: () => this.done.emit() },
  );

  protected form = this.formBuilder.group({});
  protected formFields: FormField[] = [];

  protected formConfig: FormConfig = {
    submitLabel: "actie.starten",
  };

  constructor() {
    effect(() => {
      const users = this.usersQuery.data();
      if (!users) return;
      this.updateUserOptions(users);
    });
  }

  async ngOnInit() {
    const planItem = this.planItem();
    if (planItem?.type !== "HUMAN_TASK") {
      return;
    }

    try {
      const formFields =
        await this.taakFormulierenService.getAngularRequestFormBuilder(
          this.zaak(),
          planItem,
        );

      formFields.forEach((formField) => {
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

      const groups = await this.queryClient.fetchQuery(
        this.identityService.listBehandelaarGroupsForZaaktype(
          this.zaak().zaaktype.omschrijving!,
        ),
      );

      if (planItem.groepId) {
        const defaultGroup = groups.find(
          (group) => group.id === planItem.groepId,
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
        this.selectedGroupId.set(groupControl.value.id);
      }
      groupControl.valueChanges
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe((value) => {
          userControl.reset();
          if (!value) {
            userControl.disable();
            this.selectedGroupId.set(undefined);
            return;
          }
          userControl.enable();
          if (!value.id) return;
          this.selectedGroupId.set(value.id);
        });
    } catch (error) {
      // The query cache already shows the error dialog for a failed read.
      if (error instanceof HttpErrorResponse) return;
      this.foutAfhandelingService.openFoutDialog(
        error instanceof Error ? error.message : String(error),
      );
    }
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

  protected onFormSubmit(formGroup: FormGroup) {
    this.doHumanTaskPlanItemMutation.mutate({
      planItemInstanceId: this.planItem()!.id!,
      groep: this.form.get("group")!.value!,
      medewerker: this.form.get("user")!.value!,
      fataledatum: this.form.get("taakFataledatum")?.value,
      taakdata: mapFormGroupToTaskData(formGroup, {
        ignoreKeys: ["group", "user"],
      }),
    });
  }
}
