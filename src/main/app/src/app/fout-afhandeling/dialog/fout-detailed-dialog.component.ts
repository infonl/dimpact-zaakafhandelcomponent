/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, Inject } from "@angular/core";
import { MAT_DIALOG_DATA, MatDialogRef } from "@angular/material/dialog";
import {TranslateModule} from "@ngx-translate/core";
import {MaterialModule} from "../../shared/material/material.module";
import {NgIf} from "@angular/common";

@Component({
  standalone: true,
  templateUrl: "fout-detailed-dialog.component.html",
  imports: [TranslateModule, MaterialModule, NgIf],
  styles: `
    .details {
      background: #EEEEEE;
      padding: 12px 12px 12px;
      overflow-x: scroll;
    }
  `
})
export class FoutDetailedDialogComponent {
  constructor(
    public dialogRef: MatDialogRef<FoutDetailedDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: {
      error: string
      details: string
    },
  ) {}

  panelOpenState = false;

  close(): void {
    this.dialogRef.close();
  }
}
