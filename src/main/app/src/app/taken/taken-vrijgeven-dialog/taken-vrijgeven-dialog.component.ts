/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NgIf } from "@angular/common";
import { Component, inject } from "@angular/core";
import { FormBuilder, ReactiveFormsModule, Validators } from "@angular/forms";
import {
  MAT_DIALOG_DATA,
  MatDialogModule,
  MatDialogRef,
} from "@angular/material/dialog";
import { MatDividerModule } from "@angular/material/divider";
import { MatIconModule } from "@angular/material/icon";
import { MatToolbarModule } from "@angular/material/toolbar";
import { TranslateModule } from "@ngx-translate/core";
import { injectMutation } from "@tanstack/angular-query-experimental";
import { ZacFormActions } from "../../shared/form/form-actions/form-actions.component";
import { ZacInput } from "../../shared/form/input/input";
import { TaakZoekObject } from "../../zoeken/model/taken/taak-zoek-object";
import { TakenService } from "../taken.service";

@Component({
  selector: "zac-taken-vrijgeven-dialog",
  templateUrl: "./taken-vrijgeven-dialog.component.html",
  styleUrls: ["./taken-vrijgeven-dialog.component.less"],
  standalone: true,
  imports: [
    NgIf,
    ReactiveFormsModule,
    MatToolbarModule,
    MatIconModule,
    MatDialogModule,
    MatDividerModule,
    TranslateModule,
    ZacInput,
    ZacFormActions,
  ],
})
export class TakenVrijgevenDialogComponent {
  private readonly dialogRef = inject(MatDialogRef);
  private readonly takenService = inject(TakenService);
  private readonly formBuilder = inject(FormBuilder);
  protected readonly data = inject<{
    taken: TaakZoekObject[];
    screenEventResourceId: string;
  }>(MAT_DIALOG_DATA);

  protected readonly mutation = injectMutation(() => ({
    ...this.takenService.vrijgevenVanuitLijst(),
    onSuccess: () => this.dialogRef.close(true),
    onMutate: () => {
      this.dialogRef.disableClose = true;
    },
    onSettled: () => {
      this.dialogRef.disableClose = false;
    },
  }));

  protected readonly form = this.formBuilder.group({
    reden: this.formBuilder.control<string | null>(null, [
      Validators.maxLength(100),
      Validators.required,
    ]),
  });

  constructor() {
    if (this.data.taken.length) return;
    this.form.disable();
  }

  protected close() {
    this.dialogRef.close(false);
  }

  protected vrijgeven() {
    this.mutation.mutate({
      reden: this.form.value.reden,
      screenEventResourceId: this.data.screenEventResourceId,
      taken: this.data.taken
        .filter(({ behandelaarGebruikersnaam }) => !!behandelaarGebruikersnaam)
        .map(({ zaakUuid, id }) => ({
          taakId: id,
          zaakUuid,
        })),
    });
  }
}
