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
import { injectMutation } from "@tanstack/angular-query-experimental";
import { UtilService } from "../../../../core/service/util.service";
import { ZacFormActions } from "../../../../shared/form/form-actions/form-actions.component";
import { ZacInput } from "../../../../shared/form/input/input";
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
  private readonly utilService = inject(UtilService);

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

  protected readonly mutation = injectMutation(() => ({
    mutationFn: () =>
      this.service.updateReferentieTabelAsync(
        this.data.tabel.id!,
        this.buildBody(),
      ),
    onMutate: () => {
      this.dialogRef.disableClose = true;
    },
    onSettled: () => {
      this.dialogRef.disableClose = false;
    },
    onSuccess: () => {
      this.utilService.openSnackbar(
        this.isEdit
          ? "msg.referentietabel.waarde-gewijzigd"
          : "msg.referentietabel.waarde-toegevoegd",
        { value: this.form.getRawValue().name },
      );
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

  private buildBody(): GeneratedType<"RestReferenceTableUpdate"> {
    const { tabel, value } = this.data;
    const name = this.form.getRawValue().name;
    const existing: GeneratedType<"RestReferenceTableValue">[] =
      tabel.values ?? [];
    const values = value
      ? existing.map((current) =>
          current.id === value.id ? { ...current, name } : current,
        )
      : [...existing, { name }];
    return { code: tabel.code, name: tabel.name, values };
  }
}
