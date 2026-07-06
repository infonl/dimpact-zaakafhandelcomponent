/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HttpErrorResponse } from "@angular/common/http";
import { Component, inject, input, signal } from "@angular/core";
import { FormGroup, ReactiveFormsModule } from "@angular/forms";
import { MatButtonModule } from "@angular/material/button";
import { MatDialogModule, MatDialogRef } from "@angular/material/dialog";
import { MatDividerModule } from "@angular/material/divider";
import { MatIconModule } from "@angular/material/icon";
import { MatToolbarModule } from "@angular/material/toolbar";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { injectMutation } from "@tanstack/angular-query-experimental";
import { lastValueFrom, Observable } from "rxjs";
import { ZacFormActions } from "../../form/form-actions/form-actions.component";

@Component({
  selector: "zac-dialog-body",
  templateUrl: "./dialog-body.component.html",
  styleUrls: ["./dialog-body.component.less"],
  standalone: true,
  imports: [
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
export class ZacDialogBody {
  private readonly dialogRef = inject(MatDialogRef);
  private readonly translateService = inject(TranslateService);

  readonly form = input.required<FormGroup>();
  readonly callback = input.required<() => Observable<unknown>>();
  readonly melding = input<string>();
  readonly uitleg = input<string>();
  readonly icon = input<string>();
  readonly confirmLabel = input("actie.ja");
  readonly cancelLabel = input("actie.annuleren");

  protected readonly errorMessage = signal<string | null>(null);

  protected readonly mutation = injectMutation(() => ({
    mutationFn: () => lastValueFrom(this.callback()()),
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
    const form = this.form();
    if (
      form.disabled ||
      !form.valid ||
      !form.dirty ||
      this.mutation.isPending()
    ) {
      return;
    }
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
