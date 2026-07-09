/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { AsyncPipe, NgForOf, NgIf } from "@angular/common";
import { Component, Inject } from "@angular/core";
import { MAT_DIALOG_DATA, MatDialogRef } from "@angular/material/dialog";
import { TranslateModule } from "@ngx-translate/core";
import { ReferentieTabelService } from "src/app/admin/referentie-tabel.service";
import { MaterialModule } from "../../shared/material/material.module";

@Component({
  standalone: true,
  templateUrl: "fout-detailed-dialog.component.html",
  styleUrls: ["./fout-detailed-dialog.component.less"],
  imports: [TranslateModule, MaterialModule, NgIf, AsyncPipe, NgForOf],
})
export class FoutDetailedDialogComponent {
  constructor(
    private referentieTabelService: ReferentieTabelService,
    public dialogRef: MatDialogRef<FoutDetailedDialogComponent>,
    @Inject(MAT_DIALOG_DATA)
    public data: {
      error: string;
      details: string;
      showServerErrorTexts?: boolean;
    },
  ) {}

  serverErrorTexts = this.referentieTabelService.listServerErrorTexts();

  close(): void {
    this.dialogRef.close();
  }
}
