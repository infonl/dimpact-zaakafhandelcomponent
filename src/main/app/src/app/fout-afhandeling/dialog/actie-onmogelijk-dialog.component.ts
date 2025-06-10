/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component } from "@angular/core";
import { MatDialogRef } from "@angular/material/dialog";

@Component({
  templateUrl: "actie-onmogelijk-dialog.component.html",
})
export class ActieOnmogelijkDialogComponent {
  constructor(public dialogRef: MatDialogRef<ActieOnmogelijkDialogComponent>) {}

  close(): void {
    this.dialogRef.close();
  }
}
