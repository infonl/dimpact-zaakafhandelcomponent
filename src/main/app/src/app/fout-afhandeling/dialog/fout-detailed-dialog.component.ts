/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { AsyncPipe } from "@angular/common";
import { Component, Inject } from "@angular/core";
import { MatButtonModule } from "@angular/material/button";
import {
  MAT_DIALOG_DATA,
  MatDialogModule,
  MatDialogRef,
} from "@angular/material/dialog";
import { MatDividerModule } from "@angular/material/divider";
import { MatIconModule } from "@angular/material/icon";
import { MatToolbarModule } from "@angular/material/toolbar";
import { TranslateModule } from "@ngx-translate/core";
import { ReferentieTabelService } from "src/app/admin/referentie-tabel.service";

@Component({
  templateUrl: "fout-detailed-dialog.component.html",
  imports: [
    TranslateModule,
    MatButtonModule,
    MatDialogModule,
    MatDividerModule,
    MatIconModule,
    MatToolbarModule,
    AsyncPipe,
  ],
})
export class FoutDetailedDialogComponent {
  constructor(
    private referentieTabelService: ReferentieTabelService,
    public dialogRef: MatDialogRef<FoutDetailedDialogComponent>,
    @Inject(MAT_DIALOG_DATA)
    public data: {
      error: string;
      details: string;
      showServerErrorTexts?: boolean;
    },
  ) {}

  serverErrorTexts = this.referentieTabelService.listServerErrorTexts();

  close(): void {
    this.dialogRef.close();
  }
}
