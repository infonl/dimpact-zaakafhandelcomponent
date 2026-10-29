/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, inject, ChangeDetectionStrategy } from "@angular/core";
import { MatButtonModule } from "@angular/material/button";
import {
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
  templateUrl: "actie-onmogelijk-dialog.component.html",
  host: { "data-dialog-id": "actie" },
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
export class ActieOnmogelijkDialogComponent {
  private readonly dialogRef =
    inject<MatDialogRef<ActieOnmogelijkDialogComponent>>(MatDialogRef);

  protected close(): void {
    this.dialogRef.close();
  }
}
