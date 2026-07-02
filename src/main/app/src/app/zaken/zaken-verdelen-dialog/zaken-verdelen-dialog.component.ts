/*
 * SPDX-FileCopyrightText: 2021 - 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NgIf } from "@angular/common";
import { Component, computed, inject } from "@angular/core";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { FormBuilder, ReactiveFormsModule, Validators } from "@angular/forms";
import { MatButtonModule } from "@angular/material/button";
import {
  MAT_DIALOG_DATA,
  MatDialogModule,
  MatDialogRef,
} from "@angular/material/dialog";
import { MatDividerModule } from "@angular/material/divider";
import { MatError } from "@angular/material/form-field";
import { MatIconModule } from "@angular/material/icon";
import { MatProgressSpinnerModule } from "@angular/material/progress-spinner";
import { MatToolbarModule } from "@angular/material/toolbar";
import { TranslateModule } from "@ngx-translate/core";
import { injectMutation } from "@tanstack/angular-query-experimental";
import { IdentityService } from "../../identity/identity.service";
import { ZacAutoComplete } from "../../shared/form/auto-complete/auto-complete";
import { ZacInput } from "../../shared/form/input/input";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZaakZoekObject } from "../../zoeken/model/zaken/zaak-zoek-object";
import { ZakenService } from "../zaken.service";

@Component({
  templateUrl: "zaken-verdelen-dialog.component.html",
  styleUrls: ["./zaken-verdelen-dialog.component.less"],
  standalone: true,
  imports: [
    NgIf,
    ReactiveFormsModule,
    MatToolbarModule,
    MatIconModule,
    MatButtonModule,
    MatDividerModule,
    MatDialogModule,
    MatProgressSpinnerModule,
    MatError,
    TranslateModule,
    ZacAutoComplete,
    ZacInput,
  ],
})
export class ZakenVerdelenDialogComponent {
  private readonly dialogRef = inject(
    MatDialogRef<ZakenVerdelenDialogComponent>,
  );
  private readonly zakenService = inject(ZakenService);
  private readonly formBuilder = inject(FormBuilder);
  private readonly identityService = inject(IdentityService);

  protected readonly data = inject<ZaakZoekObject[]>(MAT_DIALOG_DATA);

  protected readonly mutation = injectMutation(() => ({
    ...this.zakenService.verdelenVanuitLijst(),
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

  protected readonly groupsQuery = injectMutation(() =>
    this.identityService.listBehandelaarGroupsForZaaktypesQuery(),
  );

  protected readonly noAuthorisedGroups = computed(() => {
    const groupsQuery = this.groupsQuery.data();
    return groupsQuery !== undefined && groupsQuery.length === 0;
  });

  protected users: GeneratedType<"RestUser">[] = [];

  constructor() {
    this.groupsQuery.mutate({
      zaaktypeDescriptions: this.data.map(
        ({ zaaktypeOmschrijving }) => zaaktypeOmschrijving,
      ),
    });

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

  protected isDisabled() {
    return this.form.invalid || this.mutation.isPending() || !this.data.length;
  }

  protected verdeel() {
    this.dialogRef.disableClose = true;
    this.mutation.mutate({
      uuids: this.data.map(({ id }) => id),
      screenEventResourceId: crypto.randomUUID(),
      groepId: this.form.value.groep!.id,
      behandelaarGebruikersnaam: this.form.value.medewerker?.id,
      reden: this.form.value.reden,
    });
  }
}
