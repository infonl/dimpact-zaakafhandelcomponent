/*
 * SPDX-FileCopyrightText: 2021 - 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, inject } from "@angular/core";
import { FormBuilder, Validators } from "@angular/forms";
import { MAT_DIALOG_DATA, MatDialogRef } from "@angular/material/dialog";
import { injectMutation } from "@tanstack/angular-query-experimental";
import { ZaakZoekObject } from "../../zoeken/model/zaken/zaak-zoek-object";
import { ZakenService } from "../zaken.service";

@Component({
  templateUrl: "zaken-vrijgeven-dialog.component.html",
  styleUrls: ["./zaken-vrijgeven-dialog.component.less"],
})
export class ZakenVrijgevenDialogComponent {
  private readonly dialogRef = inject(MatDialogRef);
  private readonly zakenService = inject(ZakenService);
  private readonly formBuilder = inject(FormBuilder);
  protected readonly data = inject<ZaakZoekObject[]>(MAT_DIALOG_DATA);

  protected readonly mutation = injectMutation(() => ({
    ...this.zakenService.vrijgevenVanuitLijst(),
    onSuccess: () => this.dialogRef.close(true),
    onMutate: () => {
      this.dialogRef.disableClose = true
    }
  }));

  protected readonly form = this.formBuilder.group({
    reden: this.formBuilder.control<string | null>(null, [
      Validators.maxLength(100),
    ]),
  });

  close() {
    this.dialogRef.close(false);
  }

  vrijgeven() {
    this.mutation.mutate({
      uuids: this.data
        .filter(({ behandelaarGebruikersnaam }) => !!behandelaarGebruikersnaam)
        .map(({ id }) => id),
      reden: this.form.value.reden,
    });
  }
}
