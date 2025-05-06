/*
 * SPDX-FileCopyrightText: 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, Inject } from "@angular/core";
import { MAT_DIALOG_DATA, MatDialogRef } from "@angular/material/dialog";

@Component({
  templateUrl: "actie-onmogelijk-dialog.component.html",
  styleUrls: ["./dialog.component.less"],
})
export class ActieOnmogelijkDialogComponent {
  constructor(
    public dialogRef: MatDialogRef<ActieOnmogelijkDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: string,
  ) {}

  close(): void {
    this.dialogRef.close();
  }
}
