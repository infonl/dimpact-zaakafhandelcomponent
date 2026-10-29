/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { ChangeDetectionStrategy, Component, inject } from "@angular/core";
import { MatButtonModule } from "@angular/material/button";
import {
  MAT_DIALOG_DATA,
  MatDialogModule,
  MatDialogRef,
} from "@angular/material/dialog";
import { MatDividerModule } from "@angular/material/divider";
import { MatIconModule } from "@angular/material/icon";
import { MatToolbarModule } from "@angular/material/toolbar";
import { TranslatePipe } from "@ngx-translate/core";
import { injectQuery } from "@tanstack/angular-query-experimental";
import { ReferentieTabelService } from "src/app/admin/referentie-tabel.service";

@Component({
  templateUrl: "fout-detailed-dialog.component.html",
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [
    TranslatePipe,
    MatButtonModule,
    MatDialogModule,
    MatDividerModule,
    MatIconModule,
    MatToolbarModule,
  ],
})
export class FoutDetailedDialogComponent {
  private readonly referentieTabelService = inject(ReferentieTabelService);
  private readonly dialogRef = inject(
    MatDialogRef<FoutDetailedDialogComponent>,
  );
  protected readonly data = inject<{
    error: string;
    details: string;
    showServerErrorTexts?: boolean;
  }>(MAT_DIALOG_DATA);

  protected readonly serverErrorTextsQuery = injectQuery(() => ({
    ...this.referentieTabelService.listServerErrorTextsQuery(),
    enabled: Boolean(this.data.showServerErrorTexts),
    // Shown inside the error dialog, so a failure must not open another one.
    meta: { reportErrors: false },
    retry: false,
  }));

  close(): void {
    this.dialogRef.close();
  }
}
