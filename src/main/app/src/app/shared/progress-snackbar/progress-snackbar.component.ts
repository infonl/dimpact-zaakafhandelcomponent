/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, Signal, computed, inject } from "@angular/core";
import {
  MatProgressBar,
  ProgressBarMode,
} from "@angular/material/progress-bar";
import {
  MAT_SNACK_BAR_DATA,
  MatSnackBarLabel,
  MatSnackBarRef,
} from "@angular/material/snack-bar";

@Component({
  selector: "app-progress-snackbar",
  imports: [MatSnackBarLabel, MatProgressBar],
  templateUrl: "./progress-snackbar.component.html",
  styleUrls: ["./progress-snackbar.component.less"],
})
export class ProgressSnackbar {
  readonly snackBarRef = inject(MatSnackBarRef);
  readonly data = inject<{
    progressPercentage: Signal<number>;
    message: string;
  }>(MAT_SNACK_BAR_DATA);

  progressMode = computed<ProgressBarMode>(() => {
    const percentage = this.data.progressPercentage();
    return percentage === 100 || percentage === 0 ? "query" : "determinate";
  });
}
