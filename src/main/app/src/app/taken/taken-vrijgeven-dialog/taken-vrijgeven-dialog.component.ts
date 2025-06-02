/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, Inject } from "@angular/core";
import { MAT_DIALOG_DATA, MatDialogRef } from "@angular/material/dialog";
import { InputFormFieldBuilder } from "../../shared/material-form-builder/form-components/input/input-form-field-builder";
import { TaakZoekObject } from "../../zoeken/model/taken/taak-zoek-object";
import { TakenService } from "../taken.service";

@Component({
  selector: "zac-taken-vrijgeven-dialog",
  templateUrl: "./taken-vrijgeven-dialog.component.html",
  styleUrls: ["./taken-vrijgeven-dialog.component.less"],
})
export class TakenVrijgevenDialogComponent {
  loading = false;
  redenFormField = new InputFormFieldBuilder()
    .id("reden")
    .label("reden")
    .maxlength(100)
    .build();

  constructor(
    public dialogRef: MatDialogRef<TakenVrijgevenDialogComponent>,
    @Inject(MAT_DIALOG_DATA)
    public data: {
      taken: TaakZoekObject[];
      screenEventResourceId: string;
    },
    private takenService: TakenService,
  ) {}

  close() {
    this.dialogRef.close(false);
  }

  vrijgeven() {
    this.redenFormField.readonly = true;
    this.dialogRef.disableClose = true;
    this.loading = true;
    const reden = this.redenFormField.formControl.value;
    this.takenService
      .vrijgevenVanuitLijst({
        reden,
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
        this.dialogRef.close(reden);
      });
  }
}
