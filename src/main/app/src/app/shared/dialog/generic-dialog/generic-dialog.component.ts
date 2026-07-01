/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NgTemplateOutlet } from "@angular/common";
import { HttpErrorResponse } from "@angular/common/http";
import { Component, inject, signal } from "@angular/core";
import { ReactiveFormsModule } from "@angular/forms";
import { MatButtonModule } from "@angular/material/button";
import {
  MAT_DIALOG_DATA,
  MatDialogModule,
  MatDialogRef,
} from "@angular/material/dialog";
import { MatDividerModule } from "@angular/material/divider";
import { MatIconModule } from "@angular/material/icon";
import { MatToolbarModule } from "@angular/material/toolbar";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { injectMutation } from "@tanstack/angular-query-experimental";
import { lastValueFrom } from "rxjs";
import { ZacFormActions } from "../../form/form-actions/form-actions.component";
import { GenericDialogData } from "./generic-dialog-data";

/**
 * Reusable confirmation dialog that renders caller-supplied `zac-*` fields (projected through
 * {@link GenericDialogData.contentTemplate}) and keeps the window open on error so the action
 * can be retried. Open it with `MatDialog.open(GenericDialogComponent, { data })`.
 */
@Component({
  selector: "zac-generic-dialog",
  templateUrl: "./generic-dialog.component.html",
  styleUrls: ["./generic-dialog.component.less"],
  standalone: true,
  imports: [
    NgTemplateOutlet,
    ReactiveFormsModule,
    MatButtonModule,
    MatToolbarModule,
    MatIconModule,
    MatDialogModule,
    MatDividerModule,
    TranslateModule,
    ZacFormActions,
  ],
})
export class GenericDialogComponent {
  private readonly dialogRef = inject(MatDialogRef<GenericDialogComponent>);
  private readonly translateService = inject(TranslateService);
  protected readonly data = inject<GenericDialogData>(MAT_DIALOG_DATA);

  protected readonly errorMessage = signal<string | null>(null);

  protected readonly mutation = injectMutation(() => ({
    mutationFn: () => lastValueFrom(this.data.callback(this.data.form)),
    onMutate: () => {
      this.errorMessage.set(null);
      this.dialogRef.disableClose = true;
    },
    onSuccess: (result) => this.dialogRef.close(result ?? true),
    onError: (error) => this.errorMessage.set(this.toErrorMessage(error)),
    onSettled: () => {
      this.dialogRef.disableClose = false;
    },
  }));

  protected confirm() {
    if (this.data.form.invalid) return;
    this.mutation.mutate();
  }

  protected cancel() {
    this.dialogRef.close(false);
  }

  private toErrorMessage(error: unknown): string {
    const message =
      error instanceof HttpErrorResponse
        ? (error.error?.message ?? error.message)
        : null;
    return this.translateService.instant(
      message || "dialoog.error.body.technisch",
    );
  }
}
