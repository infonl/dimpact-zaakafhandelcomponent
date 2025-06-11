/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, Inject } from "@angular/core";
import { Validators } from "@angular/forms";
import { MAT_DIALOG_DATA, MatDialogRef } from "@angular/material/dialog";
import { TextareaFormFieldBuilder } from "../../shared/material-form-builder/form-components/textarea/textarea-form-field-builder";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZakenService } from "../zaken.service";

@Component({
  templateUrl: "zaak-ontkoppelen-dialog.component.html",
})
export class ZaakOntkoppelenDialogComponent {
  protected readonly redenFormField = new TextareaFormFieldBuilder()
    .id("reden")
    .label("reden")
    .maxlength(100)
    .validators(Validators.required)
    .build();
  protected loading = false;

  constructor(
    public dialogRef: MatDialogRef<ZaakOntkoppelenDialogComponent>,
    @Inject(MAT_DIALOG_DATA)
    public data: GeneratedType<"RestZaakUnlinkData">,
    private zakenService: ZakenService,
  ) {}

  close(): void {
    this.dialogRef.close();
  }

  ontkoppel(): void {
    this.dialogRef.disableClose = true;
    this.loading = true;
    this.data.reden = this.redenFormField.formControl.value!;
    this.zakenService.ontkoppelZaak(this.data).subscribe(() => {
      this.dialogRef.close(true);
    });
  }
}
