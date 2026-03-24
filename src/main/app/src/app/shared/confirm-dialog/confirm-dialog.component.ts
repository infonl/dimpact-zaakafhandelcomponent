/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NgIf } from "@angular/common";
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
import { MatProgressSpinnerModule } from "@angular/material/progress-spinner";
import { MatToolbarModule } from "@angular/material/toolbar";
import { TranslateModule } from "@ngx-translate/core";
import { Observable } from "rxjs";

@Component({
  templateUrl: "confirm-dialog.component.html",
  styleUrls: ["./confirm-dialog.component.less"],
  standalone: true,
  imports: [
    NgIf,
    MatToolbarModule,
    MatDialogTitle,
    MatDialogContent,
    MatDialogActions,
    MatDividerModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    TranslateModule,
  ],
})
export class ConfirmDialogComponent {
  protected loading = false;

  constructor(
    private readonly dialogRef: MatDialogRef<ConfirmDialogComponent>,
    @Inject(MAT_DIALOG_DATA) protected readonly data: ConfirmDialogData,
  ) {}

  protected confirm(): void {
    if (this.data.observable) {
      this.loading = true;
      this.dialogRef.disableClose = true;
      this.data.observable.subscribe({
        next: () => this.dialogRef.close(true),
        error: () => this.dialogRef.close(false),
      });
    } else {
      this.dialogRef.close(true);
    }
  }

  protected cancel(): void {
    this.dialogRef.close(false);
  }
}

export class ConfirmDialogData {
  _melding: { key: string; args?: object };

  constructor(
    private translation: { key: string; args?: object } | string,
    public observable?: Observable<unknown>,
    public uitleg?: string,
  ) {
    if (typeof translation === "string") {
      this._melding = { key: translation, args: {} };
    } else {
      this._melding = translation;
    }
  }
}
