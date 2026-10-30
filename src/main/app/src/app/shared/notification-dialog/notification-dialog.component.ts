/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { ChangeDetectionStrategy, Component, inject } from "@angular/core";
import { MatButtonModule } from "@angular/material/button";
import {
  MAT_DIALOG_DATA,
  MatDialogActions,
  MatDialogContent,
  MatDialogRef,
} from "@angular/material/dialog";
import { TranslatePipe } from "@ngx-translate/core";

@Component({
  templateUrl: "./notification-dialog.component.html",
  standalone: true,
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [MatDialogContent, MatDialogActions, MatButtonModule, TranslatePipe],
})
export class NotificationDialogComponent {
  private readonly dialogRef =
    inject<MatDialogRef<NotificationDialogComponent>>(MatDialogRef);
  protected readonly data = inject<NotificationDialogData>(MAT_DIALOG_DATA);

  protected confirm(): void {
    this.dialogRef.close(true);
  }
}

export class NotificationDialogData {
  constructor(public melding: string) {}
}
