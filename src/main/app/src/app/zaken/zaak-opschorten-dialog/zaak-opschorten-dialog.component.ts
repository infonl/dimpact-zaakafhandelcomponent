/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, inject, signal } from "@angular/core";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { FormBuilder, ReactiveFormsModule, Validators } from "@angular/forms";
import { MatButtonModule } from "@angular/material/button";
import {
  MAT_DIALOG_DATA,
  MatDialogActions,
  MatDialogContent,
  MatDialogRef,
  MatDialogTitle,
} from "@angular/material/dialog";
import { MatDividerModule } from "@angular/material/divider";
import { MatIconModule } from "@angular/material/icon";
import { MatProgressSpinnerModule } from "@angular/material/progress-spinner";
import { MatToolbarModule } from "@angular/material/toolbar";
import { TranslatePipe } from "@ngx-translate/core";
import {
  addDays,
  daysBetween,
  PickedDate,
  startOfDay,
  toPickedDate,
} from "../../date-time";
import { trackControl } from "../../shared/form/control-state";
import { ZacDate } from "../../shared/form/date/date";
import { ZacInput } from "../../shared/form/input/input";
import { ZacTextarea } from "../../shared/form/textarea/textarea";
import { injectMutation } from "../../shared/http/inject-mutation";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZakenService } from "../zaken.service";

@Component({
  templateUrl: "zaak-opschorten-dialog.component.html",
  styleUrls: ["./zaak-opschorten-dialog.component.less"],
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatToolbarModule,
    MatDialogTitle,
    MatDialogContent,
    MatDialogActions,
    MatDividerModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    TranslatePipe,
    ZacInput,
    ZacDate,
    ZacTextarea,
  ],
})
export class ZaakOpschortenDialogComponent {
  protected readonly dialogRef =
    inject<MatDialogRef<ZaakOpschortenDialogComponent>>(MatDialogRef);
  protected readonly data = inject<{
    zaak: GeneratedType<"RestZaak">;
  }>(MAT_DIALOG_DATA);
  private readonly zakenService = inject(ZakenService);
  private readonly formBuilder = inject(FormBuilder);

  protected readonly loading = signal(true);

  protected readonly form = this.formBuilder.group({
    numberOfDays: this.formBuilder.control<number | null>(null, [
      Validators.required,
      Validators.min(1),
    ]),
    einddatumGepland: this.formBuilder.control<PickedDate | null>(null, []),
    uiterlijkeEinddatumAfdoening: this.formBuilder.control<PickedDate | null>(
      null,
      [Validators.required],
    ),
    reason: this.formBuilder.control<string | null>(null, [
      Validators.required,
      Validators.maxLength(200),
    ]),
  });

  private readonly readForm = trackControl(signal(this.form));
  protected readonly formValid = this.readForm((form) => form.valid);

  constructor() {
    const { einddatumGepland, uiterlijkeEinddatumAfdoening } = this.data.zaak;

    if (einddatumGepland) {
      this.form.controls.einddatumGepland.setValidators([
        Validators.required,
        Validators.min(startOfDay(addDays(einddatumGepland, 1)).valueOf()),
      ]);
      this.form.controls.einddatumGepland.valueChanges
        .pipe(takeUntilDestroyed())
        .subscribe((value) => {
          if (!value) return this.resetFields();
          this.updateDateFields(daysBetween(einddatumGepland, value));
        });
    }

    if (uiterlijkeEinddatumAfdoening) {
      this.form.controls.uiterlijkeEinddatumAfdoening.setValidators([
        Validators.required,
        Validators.min(
          startOfDay(addDays(uiterlijkeEinddatumAfdoening, 1)).valueOf(),
        ),
      ]);
      this.form.controls.uiterlijkeEinddatumAfdoening.valueChanges
        .pipe(takeUntilDestroyed())
        .subscribe((value) => {
          if (!value) return this.resetFields();
          this.updateDateFields(
            daysBetween(uiterlijkeEinddatumAfdoening, value),
          );
        });
    }

    this.resetFields();

    this.form.controls.numberOfDays.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe((value) => {
        this.updateDateFields(value ?? 0);
      });

    this.dialogRef
      .afterOpened()
      .pipe(takeUntilDestroyed())
      .subscribe(() => {
        this.loading.set(false);
      });
  }

  private updateDateFields(duur: number) {
    if (duur <= 0) return this.resetFields();

    const { einddatumGepland, uiterlijkeEinddatumAfdoening } = this.data.zaak;
    this.form.patchValue(
      {
        numberOfDays: duur,
        einddatumGepland: einddatumGepland
          ? addDays(einddatumGepland, duur)
          : null,
        uiterlijkeEinddatumAfdoening: uiterlijkeEinddatumAfdoening
          ? addDays(uiterlijkeEinddatumAfdoening, duur)
          : null,
      },
      { emitEvent: false },
    );
  }

  private resetFields() {
    this.form.setValue(
      {
        numberOfDays: null,
        einddatumGepland: toPickedDate(this.data.zaak.einddatumGepland),
        uiterlijkeEinddatumAfdoening: toPickedDate(
          this.data.zaak.uiterlijkeEinddatumAfdoening,
        ),
        reason: null,
      },
      { emitEvent: false },
    );

    this.form.updateValueAndValidity({ emitEvent: false });
  }

  protected readonly suspendZaakMutation = injectMutation(
    () => this.zakenService.suspendZaak(this.data.zaak.uuid),
    {
      onSuccess: (result) => {
        this.dialogRef.close(result);
      },
      onError: () => {
        this.dialogRef.disableClose = false;
      },
    },
  );

  protected opschorten() {
    this.dialogRef.disableClose = true;

    const value = this.form.getRawValue();

    this.suspendZaakMutation.mutate({
      reason: value.reason!,
      numberOfDays: value.numberOfDays,
    });
  }

  protected close() {
    this.dialogRef.close();
  }
}
