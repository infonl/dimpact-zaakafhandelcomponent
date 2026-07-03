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
import { GeneratedType } from "../../../shared/utils/generated-types";

/**
 * Dialog to edit a reference table's name. The code is shown read-only as
 * context because it is the table's identifier and cannot be changed.
 * Returns the new name on save, or undefined when cancelled.
 */
@Component({
  standalone: true,
  selector: "zac-referentie-tabel-edit-dialog",
  templateUrl: "./referentie-tabel-edit-dialog.component.html",
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
export class ReferentieTabelEditDialogComponent {
  protected readonly data: GeneratedType<"RestReferenceTable"> =
    inject(MAT_DIALOG_DATA);
  protected readonly dialogRef =
    inject<MatDialogRef<ReferentieTabelEditDialogComponent, string>>(
      MatDialogRef,
    );

  protected readonly form = new FormGroup({
    // Shown disabled: the code is the table's identifier and cannot be changed.
    code: new FormControl(
      { value: this.data.code, disabled: true },
      { nonNullable: true },
    ),
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
