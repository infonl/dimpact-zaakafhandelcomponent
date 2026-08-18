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
import { ZacFormActions } from "../../../../shared/form/form-actions/form-actions.component";
import { ZacInput } from "../../../../shared/form/input/input";
import { injectServiceMutation } from "../../../../shared/http/inject-service-mutation";
import { GeneratedType } from "../../../../shared/utils/generated-types";
import { ReferentieTabelService } from "../../../referentie-tabel.service";

export interface ReferentieTabelValueDialogData {
  tabel: GeneratedType<"RestReferenceTable">;
  value?: GeneratedType<"RestReferenceTableValue">;
}

@Component({
  standalone: true,
  selector: "zac-referentie-tabel-value-dialog",
  templateUrl: "./referentie-tabel-value-dialog.component.html",
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
export class ReferentieTabelValueDialogComponent {
  protected readonly data: ReferentieTabelValueDialogData =
    inject(MAT_DIALOG_DATA);
  private readonly dialogRef =
    inject<MatDialogRef<ReferentieTabelValueDialogComponent, boolean>>(
      MatDialogRef,
    );
  private readonly service = inject(ReferentieTabelService);

  protected readonly isEdit = this.data.value != null;
  protected readonly titel = this.isEdit
    ? "referentietabel.waarde-titel-wijzigen"
    : "referentietabel.waarde-toevoegen";
  protected readonly icoon = this.isEdit ? "edit" : "add_circle";

  protected readonly form = new FormGroup({
    name: new FormControl(this.data.value?.name ?? "", {
      nonNullable: true,
      validators: [Validators.required, Validators.maxLength(1000)],
    }),
  });

  protected readonly mutation = injectServiceMutation({
    ...(this.data.value
      ? this.service.updateReferentieTabelValue(
          this.data.tabel,
          this.data.value,
        )
      : this.service.addReferentieTabelValue(this.data.tabel)),
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
