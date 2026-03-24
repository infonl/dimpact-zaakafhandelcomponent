/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, Inject } from "@angular/core";
import { MatButtonModule } from "@angular/material/button";
import {
  MAT_DIALOG_DATA,
  MatDialogActions,
  MatDialogContent,
  MatDialogRef,
} from "@angular/material/dialog";
import { TranslateModule } from "@ngx-translate/core";

@Component({
  templateUrl: "./notification-dialog.component.html",
  standalone: true,
  imports: [
    MatDialogContent,
    MatDialogActions,
    MatButtonModule,
    TranslateModule,
  ],
})
export class NotificationDialogComponent {
  constructor(
    private readonly dialogRef: MatDialogRef<NotificationDialogData>,
    @Inject(MAT_DIALOG_DATA) protected readonly data: NotificationDialogData,
  ) {}

  protected confirm(): void {
    this.dialogRef.close(true);
  }
}

export class NotificationDialogData {
  constructor(public melding: string) {}
}
