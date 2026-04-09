/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NgIf } from "@angular/common";
import { Component, Inject } from "@angular/core";
import { FormBuilder, ReactiveFormsModule, Validators } from "@angular/forms";
import { MatButtonModule } from "@angular/material/button";
import {
  MAT_DIALOG_DATA,
  MatDialogActions,
  MatDialogContent,
  MatDialogRef,
  MatDialogTitle,
} from "@angular/material/dialog";
import { MatDividerModule } from "@angular/material/divider";
import { MatIconModule } from "@angular/material/icon";
import { MatProgressSpinnerModule } from "@angular/material/progress-spinner";
import { MatToolbarModule } from "@angular/material/toolbar";
import { TranslateModule } from "@ngx-translate/core";
import { MaterialFormBuilderModule } from "../../shared/material-form-builder/material-form-builder.module";
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
    MatDialogTitle,
    MatDialogContent,
    MatDialogActions,
    MatDividerModule,
    MatIconModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    TranslateModule,
    MaterialFormBuilderModule,
  ],
})
export class TakenVrijgevenDialogComponent {
  protected loading = false;

  protected readonly form = this.formBuilder.group({
    reden: this.formBuilder.control<string | null>(null, [
      Validators.maxLength(100),
    ]),
  });

  constructor(
    private readonly dialogRef: MatDialogRef<TakenVrijgevenDialogComponent>,
    @Inject(MAT_DIALOG_DATA)
    protected readonly data: {
      taken: TaakZoekObject[];
      screenEventResourceId: string;
    },
    private readonly takenService: TakenService,
    private readonly formBuilder: FormBuilder,
  ) {}

  protected close() {
    this.dialogRef.close(false);
  }

  protected vrijgeven() {
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
