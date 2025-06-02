/*
 * SPDX-FileCopyrightText: 2021 - 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, Inject } from "@angular/core";
import { MAT_DIALOG_DATA, MatDialogRef } from "@angular/material/dialog";
import { InputFormFieldBuilder } from "../../shared/material-form-builder/form-components/input/input-form-field-builder";
import { ZaakZoekObject } from "../../zoeken/model/zaken/zaak-zoek-object";
import { ZakenService } from "../zaken.service";

@Component({
  templateUrl: "zaken-vrijgeven-dialog.component.html",
  styleUrls: ["./zaken-vrijgeven-dialog.component.less"],
})
export class ZakenVrijgevenDialogComponent {
  loading = false;
  redenFormField = new InputFormFieldBuilder()
    .id("reden")
    .label("reden")
    .maxlength(100)
    .build();

  constructor(
    public dialogRef: MatDialogRef<ZakenVrijgevenDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: ZaakZoekObject[],
    private zakenService: ZakenService,
  ) {}

  close(): void {
    this.dialogRef.close(false);
  }

  vrijgeven() {
    this.redenFormField.readonly = true;
    this.dialogRef.disableClose = true;
    this.loading = true;
    this.zakenService
      .vrijgevenVanuitLijst({
        uuids: this.data
          .filter(
            ({ behandelaarGebruikersnaam }) => !!behandelaarGebruikersnaam,
          )
          .map(({ id }) => id),
        reden: this.redenFormField.formControl.value,
      })
      .subscribe(() => {
        this.dialogRef.close(true);
      });
  }
}
