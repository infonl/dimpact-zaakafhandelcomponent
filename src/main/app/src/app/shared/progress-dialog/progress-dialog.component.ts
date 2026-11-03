/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
import { Component, computed, inject, Signal } from "@angular/core";
import { MAT_DIALOG_DATA, MatDialogRef } from "@angular/material/dialog";
import { MatProgressBarModule } from "@angular/material/progress-bar";

@Component({
  selector: "app-progress-dialog",
  templateUrl: "./progress-dialog.component.html",
  styleUrls: ["./progress-dialog.component.less"],
  imports: [MatProgressBarModule],
})
export class ProgressDialogComponent {
  readonly dialogRef =
    inject<MatDialogRef<ProgressDialogComponent>>(MatDialogRef);
  readonly data = inject<{
    message: string;
    action?: string;
    progressPercentage?: Signal<number>;
  }>(MAT_DIALOG_DATA);

  protected readonly progressMode = computed(() => {
    const percentage = this.data.progressPercentage?.();
    return percentage === 100 || percentage === 0 ? "query" : "determinate";
  });

  onAction(): void {
    this.dialogRef.close(true);
  }
}
