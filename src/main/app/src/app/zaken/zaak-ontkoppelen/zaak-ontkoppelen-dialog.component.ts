/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, Inject } from "@angular/core";
import { FormBuilder, Validators } from "@angular/forms";
import { MAT_DIALOG_DATA, MatDialogRef } from "@angular/material/dialog";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZakenService } from "../zaken.service";

@Component({
  templateUrl: "zaak-ontkoppelen-dialog.component.html",
})
export class ZaakOntkoppelenDialogComponent {
  protected loading = false;
  protected readonly form = this.formBuilder.group({
    reden: this.formBuilder.control<string>("", [
      Validators.required,
      Validators.maxLength(100),
    ]),
  });

  constructor(
    protected readonly dialogRef: MatDialogRef<ZaakOntkoppelenDialogComponent>,
    @Inject(MAT_DIALOG_DATA)
    private readonly data: Omit<GeneratedType<"RestZaakUnlinkData">, "reden">,
    private readonly zakenService: ZakenService,
    private readonly formBuilder: FormBuilder,
  ) {}

  ontkoppel(): void {
    this.dialogRef.disableClose = true;
    this.loading = true;
    this.zakenService
      .ontkoppelZaak({
        ...this.data,
        reden: this.form.value.reden ?? "",
      })
      .subscribe(() => {
        this.dialogRef.close(true);
      });
  }
}
