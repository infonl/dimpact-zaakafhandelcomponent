/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, inject } from "@angular/core";
import {
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from "@angular/forms";
import { MatButtonModule } from "@angular/material/button";
import {
  MAT_DIALOG_DATA,
  MatDialogModule,
  MatDialogRef,
} from "@angular/material/dialog";
import { MatDividerModule } from "@angular/material/divider";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatIconModule } from "@angular/material/icon";
import { MatInputModule } from "@angular/material/input";
import { MatToolbarModule } from "@angular/material/toolbar";
import { TranslateModule } from "@ngx-translate/core";

export interface ReferentieTabelValueDialogData {
  /** Current value name (empty when adding a new value). */
  naam: string;
  /** Translation key for the dialog title. */
  titel: string;
  /** Material icon shown in the dialog header. */
  icoon: string;
}

/**
 * Dialog to add or edit a reference table value's name.
 * Returns the entered name on save, or undefined when cancelled.
 */
@Component({
  standalone: true,
  selector: "zac-referentie-tabel-value-dialog",
  templateUrl: "./referentie-tabel-value-dialog.component.html",
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatDialogModule,
    MatDividerModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatToolbarModule,
    TranslateModule,
  ],
})
export class ReferentieTabelValueDialogComponent {
  protected readonly data: ReferentieTabelValueDialogData =
    inject(MAT_DIALOG_DATA);
  protected readonly dialogRef =
    inject<MatDialogRef<ReferentieTabelValueDialogComponent, string>>(
      MatDialogRef,
    );

  protected readonly form = new FormGroup({
    naam: new FormControl(this.data.naam, {
      nonNullable: true,
      validators: [Validators.required],
    }),
  });

  protected save() {
    if (this.form.invalid) {
      return;
    }
    this.dialogRef.close(this.form.getRawValue().naam);
  }
}
