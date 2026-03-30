/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, Inject } from "@angular/core";
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
import { MatToolbarModule } from "@angular/material/toolbar";
import { TranslateModule } from "@ngx-translate/core";

@Component({
  templateUrl: "fout-dialog.component.html",
  host: { "data-dialog-id": "fout" },
  standalone: true,
  imports: [
    MatToolbarModule,
    MatDialogTitle,
    MatDialogContent,
    MatDialogActions,
    MatDividerModule,
    MatButtonModule,
    MatIconModule,
    TranslateModule,
  ],
})
export class FoutDialogComponent {
  constructor(
    private dialogRef: MatDialogRef<FoutDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: string,
  ) {}

  protected close(): void {
    this.dialogRef.close();
  }
}
