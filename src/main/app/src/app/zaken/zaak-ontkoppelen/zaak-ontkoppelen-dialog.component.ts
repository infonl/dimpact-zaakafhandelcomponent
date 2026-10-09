/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NgIf } from "@angular/common";
import { Component, Inject } from "@angular/core";
import { FormBuilder, ReactiveFormsModule, Validators } from "@angular/forms";
import { MatButtonModule } from "@angular/material/button";
import {
  MAT_DIALOG_DATA,
  MatDialogModule,
  MatDialogRef,
} from "@angular/material/dialog";
import { MatDividerModule } from "@angular/material/divider";
import { MatIconModule } from "@angular/material/icon";
import { MatProgressSpinnerModule } from "@angular/material/progress-spinner";
import { MatToolbarModule } from "@angular/material/toolbar";
import { TranslateModule } from "@ngx-translate/core";
import { ZacTextarea } from "../../shared/form/textarea/textarea";
import { injectMutation } from "../../shared/http/inject-mutation";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZakenService } from "../zaken.service";

@Component({
  templateUrl: "zaak-ontkoppelen-dialog.component.html",
  standalone: true,
  imports: [
    NgIf,
    ReactiveFormsModule,
    MatToolbarModule,
    MatIconModule,
    MatButtonModule,
    MatDividerModule,
    MatDialogModule,
    MatProgressSpinnerModule,
    TranslateModule,
    ZacTextarea,
  ],
})
export class ZaakOntkoppelenDialogComponent {
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

  protected readonly ontkoppelZaakMutation = injectMutation(
    () => this.zakenService.ontkoppelZaak(),
    {
      onSuccess: () => {
        this.dialogRef.close(true);
      },
      onError: () => {
        this.dialogRef.disableClose = false;
      },
    },
  );

  protected ontkoppel() {
    this.dialogRef.disableClose = true;
    this.ontkoppelZaakMutation.mutate({
      ...this.data,
      reden: this.form.value.reden!,
    });
  }
}
