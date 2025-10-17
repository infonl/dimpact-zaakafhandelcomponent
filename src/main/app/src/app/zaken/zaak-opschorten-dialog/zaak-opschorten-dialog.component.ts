/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, Inject } from "@angular/core";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { FormBuilder, Validators } from "@angular/forms";
import { MAT_DIALOG_DATA, MatDialogRef } from "@angular/material/dialog";
import moment, { Moment } from "moment";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZakenService } from "../zaken.service";

@Component({
  templateUrl: "zaak-opschorten-dialog.component.html",
  styleUrls: ["./zaak-opschorten-dialog.component.less"],
})
export class ZaakOpschortenDialogComponent {
  loading = true;

  protected readonly form = this.formBuilder.group({
    duurDagen: this.formBuilder.control<number | null>(null, [
      Validators.required,
      Validators.min(1),
    ]),
    einddatumGepland: this.formBuilder.control<Moment | null>(null, []),
    uiterlijkeEinddatumAfdoening: this.formBuilder.control<Moment | null>(
      null,
      [Validators.required],
    ),
    redenOpschorting: this.formBuilder.control<string | null>(null, [
      Validators.required,
      Validators.maxLength(200),
    ]),
  });

  constructor(
    public readonly dialogRef: MatDialogRef<ZaakOpschortenDialogComponent>,
    @Inject(MAT_DIALOG_DATA)
    public readonly data: { zaak: GeneratedType<"RestZaak"> },
    private readonly zakenService: ZakenService,
    private readonly formBuilder: FormBuilder,
  ) {
    if (this.data.zaak.einddatumGepland) {
      this.form.controls.einddatumGepland.setValidators([
        Validators.required,
        Validators.min(
          moment(this.data.zaak.einddatumGepland)
            .add(1, "day")
            .startOf("day")
            .valueOf(),
        ),
      ]);
      this.form.controls.einddatumGepland.valueChanges
        .pipe(takeUntilDestroyed())
        .subscribe((value) => {
          if (!value) return this.resetFields();
          this.updateDateFields(
            moment(value).diff(this.data.zaak.einddatumGepland, "days"),
          );
        });
    }

    if (this.data.zaak.uiterlijkeEinddatumAfdoening) {
      this.form.controls.uiterlijkeEinddatumAfdoening.setValidators([
        Validators.required,
        Validators.min(
          moment(this.data.zaak.uiterlijkeEinddatumAfdoening)
            .add(1, "day")
            .startOf("day")
            .valueOf(),
        ),
      ]);
      this.form.controls.uiterlijkeEinddatumAfdoening.valueChanges
        .pipe(takeUntilDestroyed())
        .subscribe((value) => {
          if (!value) return this.resetFields();
          this.updateDateFields(
            moment(value).diff(
              this.data.zaak.uiterlijkeEinddatumAfdoening,
              "days",
            ),
          );
        });
    }

    this.resetFields();

    this.form.controls.duurDagen.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe((value) => {
        this.updateDateFields(value ?? 0);
      });

    this.dialogRef
      .afterOpened()
      .pipe(takeUntilDestroyed())
      .subscribe(() => {
        this.loading = false;
      });
  }

  private updateDateFields(duur: number) {
    if (duur <= 0) return this.resetFields();

    this.form.patchValue(
      {
        duurDagen: duur,
        einddatumGepland: this.data.zaak.einddatumGepland
          ? moment(this.data.zaak.einddatumGepland).add(duur, "days")
          : null,
        uiterlijkeEinddatumAfdoening: moment(
          this.data.zaak.uiterlijkeEinddatumAfdoening,
        ).add(duur, "days"),
      },
      { emitEvent: false },
    );
  }

  private resetFields() {
    this.form.setValue(
      {
        duurDagen: null,
        einddatumGepland: this.data.zaak.einddatumGepland
          ? moment(this.data.zaak.einddatumGepland)
          : null,
        uiterlijkeEinddatumAfdoening: this.data.zaak
          .uiterlijkeEinddatumAfdoening
          ? moment(this.data.zaak.uiterlijkeEinddatumAfdoening)
          : null,
        redenOpschorting: null,
      },
      { emitEvent: false },
    );

    this.form.updateValueAndValidity({ emitEvent: false });
  }

  opschorten() {
    this.dialogRef.disableClose = true;
    this.loading = true;

    const value = this.form.getRawValue();

    this.zakenService
      .opschortenZaak(this.data.zaak.uuid, {
        ...value,
        indicatieOpschorting: true,
        einddatumGepland: value.einddatumGepland?.toISOString(),
        uiterlijkeEinddatumAfdoening:
          value.uiterlijkeEinddatumAfdoening?.toISOString(),
      })
      .subscribe({
        next: (result) => {
          this.loading = false;
          this.dialogRef.close(result);
        },
        error: () => {
          this.loading = false;
          this.dialogRef.disableClose = false;
        },
      });
  }

  close() {
    this.dialogRef.close();
  }
}
