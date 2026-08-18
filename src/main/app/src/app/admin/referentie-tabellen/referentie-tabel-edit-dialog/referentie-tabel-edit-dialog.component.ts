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
import { MatIconModule } from "@angular/material/icon";
import { MatToolbarModule } from "@angular/material/toolbar";
import { TranslateModule } from "@ngx-translate/core";
import { ZacFormActions } from "../../../shared/form/form-actions/form-actions.component";
import { ZacInput } from "../../../shared/form/input/input";
import { injectServiceMutation } from "../../../shared/http/inject-service-mutation";
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
    MatIconModule,
    MatToolbarModule,
    TranslateModule,
    ZacFormActions,
    ZacInput,
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

  protected readonly form = new FormGroup({
    code: new FormControl(
      { value: this.data.code, disabled: true },
      { nonNullable: true },
    ),
    name: new FormControl(this.data.name, {
      nonNullable: true,
      validators: [Validators.required, Validators.maxLength(256)],
    }),
  });

  protected readonly mutation = injectServiceMutation({
    ...this.service.renameReferentieTabel(this.data),
    onMutate: () => {
      this.dialogRef.disableClose = true;
    },
    onSettled: () => {
      this.dialogRef.disableClose = false;
    },
    onSuccess: () => this.dialogRef.close(true),
  });

  protected submit() {
    if (this.form.invalid) {
      return;
    }
    this.mutation.mutate(this.form.getRawValue().name);
  }

  protected close() {
    this.dialogRef.close(false);
  }
}
