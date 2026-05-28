/*
 * SPDX-FileCopyrightText: 2021 - 2022 Atos, 2024 INFO.nl
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
import { ZaakZoekObject } from "../../zoeken/model/zaken/zaak-zoek-object";
import { ZakenService } from "../zaken.service";

@Component({
  templateUrl: "zaken-vrijgeven-dialog.component.html",
  styleUrls: ["./zaken-vrijgeven-dialog.component.less"],
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
export class ZakenVrijgevenDialogComponent {
  private readonly dialogRef = inject(MatDialogRef);
  private readonly zakenService = inject(ZakenService);
  private readonly formBuilder = inject(FormBuilder);
  protected readonly data = inject<ZaakZoekObject[]>(MAT_DIALOG_DATA);

  protected readonly mutation = injectMutation(() => ({
    ...this.zakenService.vrijgevenVanuitLijst(),
    onSuccess: () => this.dialogRef.close(true),
    onMutate: () => {
      this.dialogRef.disableClose = true;
    },
    onSettled: () => {
      this.dialogRef.disableClose = false;
    },
  }));

  constructor() {
    if (this.data.length) return;
    this.form.disable();
  }

  protected readonly form = this.formBuilder.group({
    reden: this.formBuilder.control<string | null>(null, [
      Validators.maxLength(100),
      Validators.required,
    ]),
  });

  protected close() {
    this.dialogRef.close(false);
  }

  protected vrijgeven() {
    this.mutation.mutate({
      uuids: this.data
        .filter(({ behandelaarGebruikersnaam }) => !!behandelaarGebruikersnaam)
        .map(({ id }) => id),
      reden: this.form.value.reden,
    });
  }
}
