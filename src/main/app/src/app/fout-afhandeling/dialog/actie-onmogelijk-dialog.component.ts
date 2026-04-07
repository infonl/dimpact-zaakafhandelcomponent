/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component } from "@angular/core";
import { MatButtonModule } from "@angular/material/button";
import {
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
  templateUrl: "actie-onmogelijk-dialog.component.html",
  host: { "data-dialog-id": "actie" },
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
export class ActieOnmogelijkDialogComponent {
  constructor(
    private dialogRef: MatDialogRef<ActieOnmogelijkDialogComponent>,
  ) {}

  protected close(): void {
    this.dialogRef.close();
  }
}
