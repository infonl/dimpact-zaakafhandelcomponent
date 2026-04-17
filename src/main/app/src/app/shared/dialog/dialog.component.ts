/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NgFor, NgIf } from "@angular/common";
import { Component, Inject, OnInit } from "@angular/core";
import { MatButton, MatIconButton } from "@angular/material/button";
import {
  MAT_DIALOG_DATA,
  MatDialogActions,
  MatDialogContent,
  MatDialogRef,
  MatDialogTitle,
} from "@angular/material/dialog";
import { MatDivider } from "@angular/material/divider";
import { MatIcon } from "@angular/material/icon";
import { MatProgressSpinner } from "@angular/material/progress-spinner";
import { MatToolbar } from "@angular/material/toolbar";
import { TranslateModule } from "@ngx-translate/core";
import { MaterialFormBuilderModule } from "../material-form-builder/material-form-builder.module";
import { FieldType } from "../material-form-builder/model/field-type.enum";
import { DialogData } from "./dialog-data";

@Component({
  templateUrl: "dialog.component.html",
  standalone: true,
  imports: [
    NgIf,
    NgFor,
    MatToolbar,
    MatDialogTitle,
    MatIcon,
    MatIconButton,
    MatDivider,
    MatDialogContent,
    MatDialogActions,
    MatButton,
    MatProgressSpinner,
    TranslateModule,
    MaterialFormBuilderModule,
  ],
})
export class DialogComponent implements OnInit {
  protected loading = true;

  constructor(
    private readonly dialogRef: MatDialogRef<DialogComponent>,
    @Inject(MAT_DIALOG_DATA) protected data: DialogData,
  ) {}

  ngOnInit(): void {
    this.dialogRef.afterOpened().subscribe(() => {
      this.loading = false;
    });
  }

  protected confirm(): void {
    this.dialogRef.disableClose = true;
    this.loading = true;

    if (!this.data.options.callback) {
      this.dialogRef.close(true);
      return;
    }

    const results: Record<string, unknown> = {};
    for (const formField of this.data.options.formFields) {
      switch (formField.fieldType) {
        case FieldType.CHECKBOX:
          results[formField.id] = !!formField.formControl.value;
          break;
        default:
          results[formField.id] = formField.formControl.value;
          break;
      }
    }
    this.data.options.callback(results).subscribe({
      next: (data) => this.dialogRef.close(data ?? true),
      error: () => this.dialogRef.close(false),
    });
  }

  protected cancel(): void {
    this.dialogRef.close(false);
  }

  protected disabled() {
    return this.loading || (this.data && this.data.formFieldsInvalid());
  }
}
