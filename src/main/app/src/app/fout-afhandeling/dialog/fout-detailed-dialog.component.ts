/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { AsyncPipe, NgForOf, NgIf } from "@angular/common";
import { Component, Inject } from "@angular/core";
import { MAT_DIALOG_DATA, MatDialogRef } from "@angular/material/dialog";
import { TranslateModule } from "@ngx-translate/core";
import { MaterialModule } from "../../shared/material/material.module";
import { Observable } from "rxjs";

@Component({
  standalone: true,
  templateUrl: "fout-detailed-dialog.component.html",
  imports: [TranslateModule, MaterialModule, NgIf, AsyncPipe, NgForOf],
  styles: `
    .details {
      padding: 12px 12px 12px;
      overflow-x: scroll;
    }
  `,
})
export class FoutDetailedDialogComponent {
  constructor(
    public dialogRef: MatDialogRef<FoutDetailedDialogComponent>,
    @Inject(MAT_DIALOG_DATA)
    public data: {
      error: string;
      details: string;
      serverErrorTexts: Observable<string[]>;
    },
  ) {}

  panelOpenState = false;

  close(): void {
    this.dialogRef.close();
  }
}
