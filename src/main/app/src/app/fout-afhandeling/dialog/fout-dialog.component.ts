/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { ChangeDetectionStrategy, Component, inject } from "@angular/core";
import { MatButtonModule } from "@angular/material/button";
import {
  MAT_DIALOG_DATA,
  MatDialogActions,
  MatDialogContent,
  MatDialogRef,
  MatDialogTitle,
} from "@angular/material/dialog";
import { MatDividerModule } from "@angular/material/divider";
import { MatIconModule } from "@angular/material/icon";
import { MatToolbarModule } from "@angular/material/toolbar";
import { TranslatePipe } from "@ngx-translate/core";

@Component({
  templateUrl: "fout-dialog.component.html",
  host: { "data-dialog-id": "fout" },
  standalone: true,
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [
    MatToolbarModule,
    MatDialogTitle,
    MatDialogContent,
    MatDialogActions,
    MatDividerModule,
    MatButtonModule,
    MatIconModule,
    TranslatePipe,
  ],
})
export class FoutDialogComponent {
  private readonly dialogRef =
    inject<MatDialogRef<FoutDialogComponent>>(MatDialogRef);
  readonly data = inject<string>(MAT_DIALOG_DATA);

  protected close(): void {
    this.dialogRef.close();
  }
}
