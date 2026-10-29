/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  inject,
  input,
  signal,
  untracked,
} from "@angular/core";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { FormBuilder, ReactiveFormsModule, Validators } from "@angular/forms";
import { MatButtonModule } from "@angular/material/button";
import { MatDividerModule } from "@angular/material/divider";
import { MatExpansionPanelActionRow } from "@angular/material/expansion";
import { MatIconModule } from "@angular/material/icon";
import { MatDrawer } from "@angular/material/sidenav";
import { MatToolbarModule } from "@angular/material/toolbar";
import { TranslatePipe, TranslateService } from "@ngx-translate/core";
import { injectQuery } from "@tanstack/angular-query-experimental";
import { IdentityService } from "../../identity/identity.service";
import { ZacFormActions } from "../../shared/form/form-actions/form-actions.component";
import { ZacSelect } from "../../shared/form/select/select";
import { ZacTextarea } from "../../shared/form/textarea/textarea";
import { injectMutation } from "../../shared/http/inject-mutation";
import { GeneratedType } from "../../shared/utils/generated-types";
import { TakenService } from "../taken.service";

@Component({
  selector: "zac-taak-edit",
  templateUrl: "./taak-edit.component.html",
  standalone: true,
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [
    ReactiveFormsModule,
    MatToolbarModule,
    MatIconModule,
    MatButtonModule,
    MatDividerModule,
    MatExpansionPanelActionRow,
    TranslatePipe,
    ZacSelect,
    ZacTextarea,
    ZacFormActions,
  ],
})
export class TaakEditComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly identityService = inject(IdentityService);
  private readonly takenService = inject(TakenService);
  private readonly translateService = inject(TranslateService);

  protected readonly sideNav = input.required<MatDrawer>();
  protected readonly task = input.required<GeneratedType<"RestTask">>();

  private readonly groupsQuery = injectQuery(() => ({
    ...this.identityService.listBehandelaarGroupsForZaaktype(
      this.task().zaaktypeOmschrijving ?? "",
    ),
    enabled: !!this.task().zaaktypeOmschrijving,
  }));

  protected readonly groups = computed(() => {
    const behandelaarGroups = this.groupsQuery.data();
    if (!behandelaarGroups) return [];
    const taskGroup = this.task().groep;
    if (taskGroup && !behandelaarGroups.some(({ id }) => id === taskGroup.id)) {
      return [taskGroup, ...behandelaarGroups];
    }
    return behandelaarGroups;
  });

  private readonly selectedGroupId = signal<string | undefined>(undefined);

  private readonly usersQuery = injectQuery(() => ({
    ...this.identityService.listUsersInGroup(this.selectedGroupId() ?? ""),
    enabled: !!this.selectedGroupId(),
  }));

  protected readonly users = computed(() => this.usersQuery.data() ?? []);

  private prefilledTask?: GeneratedType<"RestTask">;

  protected readonly groupDisplayValue = (
    group: GeneratedType<"RestGroup">,
  ): string =>
    group.isActive === false
      ? `${group.naam ?? ""} (${this.translateService.instant("inactief").toLowerCase()})`
      : (group.naam ?? "");

  protected readonly mutation = injectMutation(
    () => this.takenService.toekennen(),
    {
      onSuccess: () => {
        void this.sideNav().close();
      },
    },
  );

  protected readonly form = this.formBuilder.group({
    groep: this.formBuilder.control<GeneratedType<"RestGroup"> | null>(null, [
      Validators.required,
    ]),
    behandelaar: this.formBuilder.control<GeneratedType<"RestUser"> | null>(
      null,
    ),
    reden: this.formBuilder.control<string | null>(null, [
      Validators.maxLength(80),
    ]),
  });

  constructor() {
    effect(() => {
      const task = this.task();
      if (!this.groupsQuery.isSuccess() || this.prefilledTask === task) return;
      this.prefilledTask = task;

      untracked(() =>
        this.form.patchValue({
          groep: task.groep,
          behandelaar: task.behandelaar,
        }),
      );
    });

    effect(() => {
      const task = this.task();
      if (task.status === "AFGEROND" || !task.rechten.canToekennen) {
        untracked(() => this.form.disable());
      }
    });

    effect(() => {
      const usersLoaded =
        !!this.selectedGroupId() && this.usersQuery.isSuccess();
      untracked(() => {
        if (usersLoaded && this.form.controls.groep.enabled) {
          this.form.controls.behandelaar.enable();
        } else {
          this.form.controls.behandelaar.disable();
        }
      });
    });

    this.form.controls.groep.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe((group) => {
        this.form.controls.behandelaar.setValue(null);
        this.selectedGroupId.set(group?.id);
      });
  }

  protected formSubmit() {
    this.mutation.mutate({
      taakId: this.task().id!,
      zaakUuid: this.task().zaakUuid!,
      groepId: this.form.value.groep!.id,
      behandelaarId: this.form.value.behandelaar?.username,
      reden: this.form.value.reden,
    });
  }
}
