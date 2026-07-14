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
import { MatDialogModule, MatDialogRef } from "@angular/material/dialog";
import { MatDividerModule } from "@angular/material/divider";
import { MatIconModule } from "@angular/material/icon";
import { MatToolbarModule } from "@angular/material/toolbar";
import { TranslateModule } from "@ngx-translate/core";
import { injectMutation } from "@tanstack/angular-query-experimental";
import { UtilService } from "../../../core/service/util.service";
import { ZacFormActions } from "../../../shared/form/form-actions/form-actions.component";
import { ZacInput } from "../../../shared/form/input/input";
import { ReferentieTabelService } from "../../referentie-tabel.service";

@Component({
  standalone: true,
  selector: "zac-referentie-tabel-create-dialog",
  templateUrl: "./referentie-tabel-create-dialog.component.html",
  styleUrl: "./referentie-tabel-create-dialog.component.less",
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatDialogModule,
    MatDividerModule,
    MatIconModule,
    MatToolbarModule,
    TranslateModule,
    ZacFormActions,
    ZacInput,
  ],
})
export class ReferentieTabelCreateDialogComponent {
  private readonly dialogRef =
    inject<MatDialogRef<ReferentieTabelCreateDialogComponent, number>>(
      MatDialogRef,
    );
  private readonly service = inject(ReferentieTabelService);
  private readonly utilService = inject(UtilService);

  protected readonly form = new FormGroup({
    code: new FormControl("", {
      nonNullable: true,
      validators: [Validators.required, Validators.maxLength(256)],
    }),
    name: new FormControl("", {
      nonNullable: true,
      validators: [Validators.required, Validators.maxLength(256)],
    }),
  });

  protected readonly mutation = injectMutation(() => ({
    ...this.service.createReferentieTabelMutation(),
    onMutate: () => {
      this.dialogRef.disableClose = true;
    },
    onSettled: () => {
      this.dialogRef.disableClose = false;
    },
  }));

  protected submit() {
    if (this.form.invalid) {
      return;
    }
    const { code, name } = this.form.getRawValue();
    this.mutation.mutate(
      { code, name, systemTable: false, values: [] },
      {
        onSuccess: (created) => {
          this.utilService.openSnackbar("msg.referentietabel.toegevoegd", {
            tabel: code,
          });
          this.dialogRef.close(created.id ?? undefined);
        },
      },
    );
  }

  protected close() {
    this.dialogRef.close();
  }
}
