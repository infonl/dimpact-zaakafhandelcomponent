/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, Inject, Signal, computed, inject } from "@angular/core";
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
