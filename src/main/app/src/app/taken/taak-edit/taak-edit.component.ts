/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, effect, inject, input } from "@angular/core";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { FormBuilder, Validators } from "@angular/forms";
import { MatSidenav } from "@angular/material/sidenav";
import { injectMutation } from "@tanstack/angular-query-experimental";
import { UtilService } from "../../core/service/util.service";
import { IdentityService } from "../../identity/identity.service";
import { GeneratedType } from "../../shared/utils/generated-types";
import { TakenService } from "../taken.service";

@Component({
  selector: "zac-taak-edit",
  templateUrl: "./taak-edit.component.html",
})
export class TaakEditComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly identityService = inject(IdentityService);
  private readonly takenService = inject(TakenService);
  private readonly utilService = inject(UtilService);

  protected readonly sideNav = input.required<MatSidenav>();
  protected readonly task = input.required<GeneratedType<"RestTask">>();

  protected groups: GeneratedType<"RestGroup">[] = [];
  protected users: GeneratedType<"RestUser">[] = [];

  protected readonly mutation = injectMutation(() => ({
    ...this.takenService.toekennen(),
    onSuccess: () => {
      void this.sideNav().close();
    },
  }));

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
      this.identityService
        .listGroups(this.task().zaaktypeUUID ?? undefined)
        .subscribe((groups) => {
          const taskGroup = this.task().groep;
          if (taskGroup && !groups.find((group) => group.id === taskGroup.id)) {
            groups.unshift(taskGroup);
          }
          this.groups = groups;
          this.form.patchValue({
            groep: taskGroup,
            behandelaar: this.task().behandelaar,
          });
        });

      if (this.task().status === "AFGEROND" || !this.task().rechten.toekennen) {
        this.form.disable();
      }
    });

    this.form.controls.groep.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe((group) => {
        this.form.controls.behandelaar.setValue(null);
        this.form.controls.behandelaar.disable();
        if (!group) return;

        this.identityService.listUsersInGroup(group.id).subscribe((users) => {
          this.form.controls.behandelaar.enable();
          this.users = users;
        });
      });
  }

  formSubmit() {
    this.mutation.mutate({
      taakId: this.task().id!,
      zaakUuid: this.task().zaakUuid!,
      groepId: this.form.value.groep!.id,
      behandelaarId: this.form.value.behandelaar?.id,
      reden: this.form.value.reden,
    });
  }
}
