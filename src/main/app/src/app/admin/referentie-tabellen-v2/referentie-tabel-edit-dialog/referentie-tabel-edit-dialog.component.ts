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
import { injectMutation } from "@tanstack/angular-query-experimental";
import { UtilService } from "../../../core/service/util.service";
import { ZacFormActions } from "../../../shared/form/form-actions/form-actions.component";
import { GeneratedType } from "../../../shared/utils/generated-types";
import { ReferentieTabelService } from "../../referentie-tabel.service";

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
    ZacFormActions,
  ],
})
export class ReferentieTabelEditDialogComponent {
  protected readonly data: GeneratedType<"RestReferenceTable"> =
    inject(MAT_DIALOG_DATA);
  private readonly dialogRef =
    inject<MatDialogRef<ReferentieTabelEditDialogComponent, boolean>>(
      MatDialogRef,
    );
  private readonly service = inject(ReferentieTabelService);
  private readonly utilService = inject(UtilService);

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

  protected readonly mutation = injectMutation(() => ({
    mutationFn: () =>
      this.service.updateReferentieTabelAsync(this.data.id!, {
        code: this.data.code,
        naam: this.form.getRawValue().naam,
        waarden: this.data.waarden ?? [],
      }),
    onMutate: () => {
      this.dialogRef.disableClose = true;
    },
    onSettled: () => {
      this.dialogRef.disableClose = false;
    },
    onSuccess: () => {
      this.utilService.openSnackbar("msg.referentietabel.gewijzigd", {
        tabel: this.data.code,
      });
      this.dialogRef.close(true);
    },
  }));

  protected submit() {
    if (this.form.invalid) {
      return;
    }
    this.mutation.mutate();
  }

  protected close() {
    this.dialogRef.close(false);
  }
}
