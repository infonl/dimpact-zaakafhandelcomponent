/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, Inject, computed, Signal, inject } from "@angular/core";
import {
  MatSnackBarRef,
  MAT_SNACK_BAR_DATA,
} from "@angular/material/snack-bar";
import { MatSnackBarLabel } from "@angular/material/snack-bar";
import {
  MatProgressBar,
  ProgressBarMode,
} from "@angular/material/progress-bar";

@Component({
  standalone: true,
  selector: "app-progress-snackbar",
  imports: [MatSnackBarLabel, MatProgressBar],
  templateUrl: "./progress-snackbar.component.html",
  styleUrls: ["./progress-snackbar.component.less"],
})
export class ProgressSnackbar {
  snackBarRef = inject(MatSnackBarRef);

  progressMode = computed<ProgressBarMode>(() => {
    const percentage = this.data.progressPercentage();
    return percentage === 100 || percentage === 0 ? "query" : "determinate";
  });

  constructor(
    @Inject(MAT_SNACK_BAR_DATA)
    public data: { progressPercentage: Signal<number>; message: string },
  ) {}
}
