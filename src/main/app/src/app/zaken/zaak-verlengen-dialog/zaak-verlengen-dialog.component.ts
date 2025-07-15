/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, Inject, OnDestroy } from "@angular/core";
import { FormBuilder, Validators } from "@angular/forms";
import { MAT_DIALOG_DATA, MatDialogRef } from "@angular/material/dialog";
import { TranslateService } from "@ngx-translate/core";
import moment, { Moment } from "moment";
import { Subject, takeUntil } from "rxjs";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZakenService } from "../zaken.service";

@Component({
  templateUrl: "zaak-verlengen-dialog.component.html",
})
export class ZaakVerlengenDialogComponent implements OnDestroy {
  private readonly destroy$ = new Subject<void>();

  loading = false;

  protected readonly form = this.formBuilder.group({
    duurDagen: this.formBuilder.control<number | null>(null, [
      Validators.required,
      Validators.min(1),
      Validators.max(Number(this.data.zaak.zaaktype.verlengingstermijn)),
    ]),
    einddatumGepland: this.data.zaak.einddatumGepland
      ? this.formBuilder.control<Moment | null>(
          moment(this.data.zaak.einddatumGepland),
          [
            Validators.required,
            Validators.min(
              moment(this.data.zaak.einddatumGepland)
                .add(1, "day")
                .startOf("day")
                .valueOf(),
            ),
            Validators.max(
              moment(this.data.zaak.einddatumGepland)
                .add(this.data.zaak.zaaktype.verlengingstermijn, "day")
                .startOf("day")
                .valueOf(),
            ),
          ],
        )
      : this.formBuilder.control<Moment | null>(null),
    uiterlijkeEinddatumAfdoening: this.formBuilder.control(
      moment(this.data.zaak.uiterlijkeEinddatumAfdoening),
      [
        Validators.required,
        Validators.min(
          moment(this.data.zaak.uiterlijkeEinddatumAfdoening)
            .add(1, "day")
            .startOf("day")
            .valueOf(),
        ),
        Validators.max(
          moment(this.data.zaak.uiterlijkeEinddatumAfdoening)
            .add(this.data.zaak.zaaktype.verlengingstermijn, "day")
            .endOf("day")
            .valueOf(),
        ),
      ],
    ),
    verlengingVastleggen: this.formBuilder.control(false, []),
    redenVerlenging: this.formBuilder.control("", [
      Validators.required,
      Validators.maxLength(200),
    ]),
    takenVerlengen: this.formBuilder.control(false, []),
  });
  protected verlengduurHint: string;

  constructor(
    private readonly formBuilder: FormBuilder,
    private readonly translateService: TranslateService,
    private readonly dialogRef: MatDialogRef<ZaakVerlengenDialogComponent>,
    private readonly zakenService: ZakenService,
    @Inject(MAT_DIALOG_DATA)
    protected readonly data: { zaak: GeneratedType<"RestZaak"> },
  ) {
    this.verlengduurHint = this.translateService.instant(
      `hint.zaak.dialoog.verlengen.verlengduur${Number(this.data.zaak.zaaktype.verlengingstermijn) > 1 ? ".meervoud" : ""}`,
      {
        verlengDuur: data.zaak.zaaktype.verlengingstermijn,
      },
    );

    this.form.controls.duurDagen.valueChanges
      .pipe(takeUntil(this.destroy$))
      .subscribe((duur) => {
        this.updateDateFields(duur);
      });

    this.form.controls.einddatumGepland.valueChanges
      .pipe(takeUntil(this.destroy$))
      .subscribe((value) => {
        this.updateDateFields(
          moment(value).diff(this.data.zaak.einddatumGepland, "days"),
        );
      });

    this.form.controls.uiterlijkeEinddatumAfdoening.valueChanges
      .pipe(takeUntil(this.destroy$))
      .subscribe((value) => {
        this.updateDateFields(
          moment(value).diff(
            this.data.zaak.uiterlijkeEinddatumAfdoening,
            "days",
          ),
        );
      });
  }

  private updateDateFields(duurDagen?: number | null) {
    this.form.reset(
      {
        duurDagen,
        einddatumGepland: this.data.zaak.einddatumGepland
          ? moment(this.data.zaak.einddatumGepland).add(duurDagen ?? 0, "days")
          : null,
        uiterlijkeEinddatumAfdoening: moment(
          this.data.zaak.uiterlijkeEinddatumAfdoening,
        ).add(duurDagen ?? 0, "days"),
      },
      { emitEvent: false },
    );
    this.form.updateValueAndValidity();
  }

  verlengen() {
    this.dialogRef.disableClose = true;
    this.loading = true;
    const {
      duurDagen,
      einddatumGepland,
      uiterlijkeEinddatumAfdoening,
      redenVerlenging,
      takenVerlengen,
    } = this.form.value;

    this.zakenService
      .verlengenZaak(this.data.zaak.uuid, {
        duurDagen: duurDagen ?? undefined,
        einddatumGepland: einddatumGepland?.toISOString(),
        uiterlijkeEinddatumAfdoening:
          uiterlijkeEinddatumAfdoening?.toISOString(),
        redenVerlenging: redenVerlenging,
        takenVerlengen: takenVerlengen ?? false,
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

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
