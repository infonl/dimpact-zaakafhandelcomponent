/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
import { Component, Inject } from "@angular/core";
import { MAT_DIALOG_DATA, MatDialogRef } from "@angular/material/dialog";
import { MatProgressBarModule } from "@angular/material/progress-bar";
import { MatSnackBarLabel } from "@angular/material/snack-bar";

@Component({
  standalone: true,
  selector: "app-progress-dialog",
  templateUrl: "./progress-dialog.component.html",
  styleUrls: ["./progress-dialog.component.less"],
  imports: [MatSnackBarLabel, MatProgressBarModule],
})
export class ProgressDialogComponent {
  constructor(
    public dialogRef: MatDialogRef<ProgressDialogComponent>,
    @Inject(MAT_DIALOG_DATA)
    public data: {
      message: string;
      action?: string;
      progressPercentage?: () => number;
    },
  ) {}

  progressMode() {
    const percentage = this.data.progressPercentage?.();
    return percentage === 100 || percentage === 0 ? "query" : "determinate";
  }

  onAction(): void {
    this.dialogRef.close(true);
  }
}
