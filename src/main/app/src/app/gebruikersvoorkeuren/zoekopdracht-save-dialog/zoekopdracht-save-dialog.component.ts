/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, computed, inject } from "@angular/core";
import { toSignal } from "@angular/core/rxjs-interop";
import { FormBuilder, ReactiveFormsModule, Validators } from "@angular/forms";
import {
  MAT_DIALOG_DATA,
  MatDialogContent,
  MatDialogRef,
  MatDialogTitle,
} from "@angular/material/dialog";
import { MatDividerModule } from "@angular/material/divider";
import { MatIconModule } from "@angular/material/icon";
import { MatToolbarModule } from "@angular/material/toolbar";
import { TranslateModule } from "@ngx-translate/core";
import { injectMutation } from "@tanstack/angular-query-experimental";
import { lastValueFrom } from "rxjs";
import { UtilService } from "../../core/service/util.service";
import { ZacFormActions } from "../../shared/form/form-actions/form-actions.component";
import { ZacInput } from "../../shared/form/input/input";
import { GeneratedType } from "../../shared/utils/generated-types";
import { GebruikersvoorkeurenService } from "../gebruikersvoorkeuren.service";

@Component({
  templateUrl: "./zoekopdracht-save-dialog.component.html",
  styleUrls: ["./zoekopdracht-save-dialog.component.less"],
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatDialogTitle,
    MatDialogContent,
    MatDividerModule,
    MatIconModule,
    MatToolbarModule,
    TranslateModule,
    ZacInput,
    ZacFormActions,
  ],
})
export class ZoekopdrachtSaveDialogComponent {
  private readonly dialogRef = inject(
    MatDialogRef<ZoekopdrachtSaveDialogComponent>,
  );
  protected readonly data = inject<{
    zoekopdrachten: GeneratedType<"RESTZoekopdracht">[];
    lijstID: GeneratedType<"Werklijst">;
    zoekopdracht: unknown;
  }>(MAT_DIALOG_DATA);
  private readonly gebruikersvoorkeurenService = inject(
    GebruikersvoorkeurenService,
  );
  private readonly utilService = inject(UtilService);
  private readonly formBuilder = inject(FormBuilder);

  protected readonly form = this.formBuilder.group({
    naam: this.formBuilder.control<string | null>(null, [Validators.required]),
  });

  private readonly naamValue = toSignal(this.form.controls.naam.valueChanges, {
    initialValue: this.form.controls.naam.value,
  });

  protected readonly submitLabel = computed(() =>
    this.findExisting(this.naamValue()) ? "actie.wijzigen" : "actie.toevoegen",
  );

  protected readonly mutation = injectMutation(() => ({
    mutationFn: () => {
      const existing = this.findExisting(this.form.value.naam);
      const zoekopdracht = existing
        ? {
            ...existing,
            json: JSON.stringify(this.data.zoekopdracht),
          }
        : {
            naam: this.form.value.naam,
            json: JSON.stringify(this.data.zoekopdracht),
            lijstID: this.data.lijstID,
          };
      return lastValueFrom(
        this.gebruikersvoorkeurenService.createOrUpdateZoekOpdrachten(
          zoekopdracht as GeneratedType<"RESTZoekopdracht">,
        ),
      );
    },
    onSuccess: () => {
      this.utilService.openSnackbar("msg.zoekopdracht.opgeslagen");
      this.dialogRef.close(true);
    },
    onError: () => this.dialogRef.close(),
  }));

  protected close() {
    this.dialogRef.close();
  }

  protected opslaan() {
    this.dialogRef.disableClose = true;
    this.mutation.mutate();
  }

  private findExisting(naam: string | null | undefined) {
    if (!naam) return undefined;
    const needle = naam.toLowerCase().trim();
    return this.data.zoekopdrachten.find(
      (value) => value.naam?.toLowerCase() === needle,
    );
  }
}
