/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, inject } from "@angular/core";
import { FormBuilder, ReactiveFormsModule, Validators } from "@angular/forms";
import { MAT_DIALOG_DATA, MatDialogRef } from "@angular/material/dialog";
import { TranslateService } from "@ngx-translate/core";
import { Observable } from "rxjs";
import { toDialogErrorMessage } from "../../shared/dialog/generic-dialog/dialog-error.util";
import { GenericDialogComponent } from "../../shared/dialog/generic-dialog/generic-dialog.component";
import { ZacFormActions } from "../../shared/form/form-actions/form-actions.component";
import { ZacSelect } from "../../shared/form/select/select";
import { GeneratedType } from "../../shared/utils/generated-types";

export type ZaakAfbrekenDialogData = {
  options: Observable<GeneratedType<"RestZaakbeeindigReden">[]>;
  callback: (
    reden: GeneratedType<"RestZaakbeeindigReden">,
  ) => Observable<unknown>;
};

@Component({
  templateUrl: "./zaak-afbreken-dialog.component.html",
  standalone: true,
  imports: [
    ReactiveFormsModule,
    GenericDialogComponent,
    ZacFormActions,
    ZacSelect,
  ],
})
export class ZaakAfbrekenDialogComponent {
  private readonly dialogRef =
    inject<MatDialogRef<ZaakAfbrekenDialogComponent>>(MatDialogRef);
  protected readonly data = inject<ZaakAfbrekenDialogData>(MAT_DIALOG_DATA);
  private readonly formBuilder = inject(FormBuilder);
  private readonly translateService = inject(TranslateService);

  protected loading = false;
  protected errorMessage: string | null = null;

  protected readonly form = this.formBuilder.group({
    reden:
      this.formBuilder.control<GeneratedType<"RestZaakbeeindigReden"> | null>(
        null,
        [Validators.required],
      ),
  });

  protected submit() {
    const reden = this.form.controls.reden.value;
    if (!this.form.valid || !this.form.dirty || this.loading || !reden) return;

    this.errorMessage = null;
    this.dialogRef.disableClose = true;
    this.loading = true;
    this.data.callback(reden).subscribe({
      next: (result) => this.dialogRef.close(result ?? true),
      error: (error) => {
        this.loading = false;
        this.dialogRef.disableClose = false;
        this.errorMessage = toDialogErrorMessage(this.translateService, error);
      },
    });
  }

  protected close() {
    if (this.loading) return;
    this.dialogRef.close(false);
  }
}
