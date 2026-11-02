/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, inject } from "@angular/core";
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
import { MatProgressSpinnerModule } from "@angular/material/progress-spinner";
import { MatToolbarModule } from "@angular/material/toolbar";
import { TranslatePipe } from "@ngx-translate/core";
import { firstValueFrom, Observable } from "rxjs";
import { injectMutation } from "../http/inject-mutation";

@Component({
  templateUrl: "confirm-dialog.component.html",
  styleUrls: ["./confirm-dialog.component.less"],
  standalone: true,
  imports: [
    MatToolbarModule,
    MatDialogTitle,
    MatDialogContent,
    MatDialogActions,
    MatDividerModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    TranslatePipe,
  ],
})
export class ConfirmDialogComponent {
  private readonly dialogRef =
    inject<MatDialogRef<ConfirmDialogComponent>>(MatDialogRef);
  protected readonly data = inject<ConfirmDialogData>(MAT_DIALOG_DATA);

  private readonly confirmMutation = injectMutation(() => ({
    mutationFn: (observable: Observable<unknown>) =>
      firstValueFrom(observable, { defaultValue: undefined }),
    onSuccess: () => this.dialogRef.close(true),
    onError: () => this.dialogRef.close(false),
  }));

  protected readonly loading = this.confirmMutation.isPending;

  protected confirm(): void {
    if (this.data.observable) {
      this.dialogRef.disableClose = true;
      this.confirmMutation.mutate(this.data.observable);
    } else {
      this.dialogRef.close(true);
    }
  }

  protected cancel(): void {
    this.dialogRef.close(false);
  }
}

export class ConfirmDialogData {
  _melding: { key: string; args?: object };

  constructor(
    private translation: { key: string; args?: object } | string,
    public observable?: Observable<unknown>,
    public uitleg?: string,
    public opties: ConfirmDialogOpties = {},
  ) {
    if (typeof translation === "string") {
      this._melding = { key: translation, args: {} };
    } else {
      this._melding = translation;
    }
  }
}

export type ConfirmDialogOpties = {
  titel?: { key: string; args?: object };
  icon?: string;
  bevestigLabel?: string;
  annuleerLabel?: string;
};
