/*
 * SPDX-FileCopyrightText: 2021 - 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, Inject } from "@angular/core";
import { FormBuilder, Validators } from "@angular/forms";
import { MAT_DIALOG_DATA, MatDialogRef } from "@angular/material/dialog";
import { ZaakZoekObject } from "../../zoeken/model/zaken/zaak-zoek-object";
import { ZakenService } from "../zaken.service";

@Component({
  templateUrl: "zaken-vrijgeven-dialog.component.html",
  styleUrls: ["./zaken-vrijgeven-dialog.component.less"],
})
export class ZakenVrijgevenDialogComponent {
  loading = false;

  protected readonly form = this.formBuilder.group({
    reden: this.formBuilder.control<string | null>(null, [
      Validators.maxLength(100),
    ]),
  });

  constructor(
    public readonly dialogRef: MatDialogRef<ZakenVrijgevenDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public readonly data: ZaakZoekObject[],
    private readonly zakenService: ZakenService,
    private readonly formBuilder: FormBuilder,
  ) {}

  close(): void {
    this.dialogRef.close(false);
  }

  vrijgeven() {
    this.dialogRef.disableClose = true;
    this.loading = true;
    this.zakenService
      .vrijgevenVanuitLijst({
        uuids: this.data
          .filter(
            ({ behandelaarGebruikersnaam }) => !!behandelaarGebruikersnaam,
          )
          .map(({ id }) => id),
        reden: this.form.value.reden,
      })
      .subscribe(() => {
        this.dialogRef.close(true);
      });
  }
}
