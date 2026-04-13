/*
 * SPDX-FileCopyrightText: 2021 - 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NgIf } from "@angular/common";
import { Component, inject } from "@angular/core";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { FormBuilder, ReactiveFormsModule, Validators } from "@angular/forms";
import { MatButtonModule } from "@angular/material/button";
import {
  MAT_DIALOG_DATA,
  MatDialogContent,
  MatDialogRef,
  MatDialogTitle,
} from "@angular/material/dialog";
import { MatDividerModule } from "@angular/material/divider";
import { MatIconModule } from "@angular/material/icon";
import { MatToolbarModule } from "@angular/material/toolbar";
import { TranslateModule } from "@ngx-translate/core";
import { injectMutation } from "@tanstack/angular-query-experimental";
import { IdentityService } from "../../identity/identity.service";
import { MaterialFormBuilderModule } from "../../shared/material-form-builder/material-form-builder.module";
import { GeneratedType } from "../../shared/utils/generated-types";
import { TaakZoekObject } from "../../zoeken/model/taken/taak-zoek-object";
import { TakenService } from "../taken.service";

@Component({
  selector: "zac-taken-verdelen-dialog",
  templateUrl: "./taken-verdelen-dialog.component.html",
  standalone: true,
  imports: [
    NgIf,
    ReactiveFormsModule,
    MatToolbarModule,
    MatIconModule,
    MatDialogTitle,
    MatDialogContent,
    MatDividerModule,
    MatButtonModule,
    TranslateModule,
    MaterialFormBuilderModule,
  ],
})
export class TakenVerdelenDialogComponent {
  private readonly dialogRef = inject(MatDialogRef);
  private readonly takenService = inject(TakenService);
  private readonly formBuilder = inject(FormBuilder);
  private readonly identityService = inject(IdentityService);

  protected readonly data = inject<{
    taken: TaakZoekObject[];
    screenEventResourceId: string;
  }>(MAT_DIALOG_DATA);

  protected readonly mutation = injectMutation(() => ({
    ...this.takenService.verdelenVanuitLijst(),
    onSuccess: () => this.dialogRef.close(this.form.value),
  }));

  protected readonly form = this.formBuilder.group({
    groep: this.formBuilder.control<GeneratedType<"RestGroup"> | null>(null, [
      Validators.required,
    ]),
    medewerker: this.formBuilder.control<GeneratedType<"RestUser"> | null>(
      null,
    ),
    reden: this.formBuilder.control<string | null>(null, [
      Validators.maxLength(100),
    ]),
  });

  protected groups = this.identityService.listGroups();
  protected users: GeneratedType<"RestUser">[] = [];

  constructor() {
    if (!this.data.taken.length) {
      this.form.disable();
      return;
    }

    this.form.controls.medewerker.disable();

    this.form.controls.groep.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe((group) => {
        this.form.controls.medewerker.setValue(null);
        this.form.controls.medewerker.disable();
        if (!group) return;

        this.identityService.listUsersInGroup(group.id).subscribe((users) => {
          this.form.controls.medewerker.enable();
          this.users = users;
        });
      });
  }

  protected close() {
    this.dialogRef.close(false);
  }

  protected verdeel() {
    this.mutation.mutate({
      taken: this.data.taken.map(({ id, zaakUuid }) => ({
        taakId: id,
        zaakUuid,
      })),
      behandelaarGebruikersnaam: this.form.value.medewerker?.id,
      reden: this.form.value.reden ?? "",
      groepId: this.form.value.groep!.id,
      screenEventResourceId: this.data.screenEventResourceId,
    });
  }
}
