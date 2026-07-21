/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, inject } from "@angular/core";
import { FormBuilder, ReactiveFormsModule, Validators } from "@angular/forms";
import {
  MAT_DIALOG_DATA,
  MatDialogActions,
  MatDialogRef,
} from "@angular/material/dialog";
import { Observable } from "rxjs";
import { ZacFormActions } from "../../form/form-actions/form-actions.component";
import { ZacInput } from "../../form/input/input";
import { ZacTextarea } from "../../form/textarea/textarea";
import { GenericDialogComponent } from "../generic-dialog/generic-dialog.component";

export type RedenDialogData = {
  titleKey: string;
  icon?: string;
  label?: string;
  multiline?: boolean;
  maxlength?: number;
  melding?: string;
  uitleg?: string;
  confirmButtonActionKey?: string | null;
  cancelButtonActionKey?: string | null;
  callback?: (reden: string) => Observable<unknown>;
};

@Component({
  templateUrl: "./reden-dialog-form.component.html",
  standalone: true,
  imports: [
    ReactiveFormsModule,
    GenericDialogComponent,
    MatDialogActions,
    ZacFormActions,
    ZacInput,
    ZacTextarea,
  ],
})
export class RedenDialogFormComponent {
  private readonly dialogRef =
    inject<MatDialogRef<RedenDialogFormComponent>>(MatDialogRef);
  protected readonly data = inject<RedenDialogData>(MAT_DIALOG_DATA);
  private readonly formBuilder = inject(FormBuilder);

  protected loading = false;
  protected readonly label = this.data.label ?? "reden";
  protected readonly submitLabel =
    this.data.confirmButtonActionKey ?? "actie.ja";
  protected readonly cancelLabel =
    this.data.cancelButtonActionKey ?? "actie.annuleren";

  protected readonly form = this.formBuilder.group({
    reden: this.formBuilder.control<string | null>(
      null,
      this.data.maxlength
        ? [Validators.required, Validators.maxLength(this.data.maxlength)]
        : [Validators.required],
    ),
  });

  protected submit() {
    if (!this.form.valid || !this.form.dirty || this.loading) return;

    const reden = this.form.controls.reden.value ?? "";
    if (!this.data.callback) {
      this.dialogRef.close(true);
      return;
    }

    this.dialogRef.disableClose = true;
    this.loading = true;
    this.data.callback(reden).subscribe({
      next: (result) => this.dialogRef.close(result ?? true),
      error: () => this.dialogRef.close(false),
    });
  }

  protected close() {
    if (this.loading) return;
    this.dialogRef.close(false);
  }
}
