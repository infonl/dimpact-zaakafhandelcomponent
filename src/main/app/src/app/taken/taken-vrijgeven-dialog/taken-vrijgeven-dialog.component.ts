/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, Inject } from "@angular/core";
import { FormBuilder, Validators } from "@angular/forms";
import { MAT_DIALOG_DATA, MatDialogRef } from "@angular/material/dialog";
import { TaakZoekObject } from "../../zoeken/model/taken/taak-zoek-object";
import { TakenService } from "../taken.service";

@Component({
  selector: "zac-taken-vrijgeven-dialog",
  templateUrl: "./taken-vrijgeven-dialog.component.html",
  styleUrls: ["./taken-vrijgeven-dialog.component.less"],
})
export class TakenVrijgevenDialogComponent {
  loading = false;

  protected readonly form = this.formBuilder.group({
    reden: this.formBuilder.control<string | null>(null, [
      Validators.maxLength(100),
    ]),
  });

  constructor(
    public readonly dialogRef: MatDialogRef<TakenVrijgevenDialogComponent>,
    @Inject(MAT_DIALOG_DATA)
    public readonly data: {
      taken: TaakZoekObject[];
      screenEventResourceId: string;
    },
    private readonly takenService: TakenService,
    private readonly formBuilder: FormBuilder,
  ) {}

  close() {
    this.dialogRef.close(false);
  }

  vrijgeven() {
    this.dialogRef.disableClose = true;
    this.loading = true;
    this.takenService
      .vrijgevenVanuitLijst({
        reden: this.form.value.reden,
        screenEventResourceId: this.data.screenEventResourceId,
        taken: this.data.taken
          .filter(
            ({ behandelaarGebruikersnaam }) => !!behandelaarGebruikersnaam,
          )
          .map(({ zaakUuid, id }) => ({
            taakId: id,
            zaakUuid,
          })),
      })
      .subscribe(() => {
        this.dialogRef.close(true);
      });
  }
}
