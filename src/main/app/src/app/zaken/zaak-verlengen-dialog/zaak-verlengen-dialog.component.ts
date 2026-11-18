/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, inject, OnDestroy, signal } from "@angular/core";
import { FormBuilder, ReactiveFormsModule, Validators } from "@angular/forms";
import { MatButtonModule } from "@angular/material/button";
import {
  MAT_DIALOG_DATA,
  MatDialogContent,
  MatDialogRef,
  MatDialogTitle,
} from "@angular/material/dialog";
import { MatDividerModule } from "@angular/material/divider";
import { MatFormFieldModule, MatHint } from "@angular/material/form-field";
import { MatIconModule } from "@angular/material/icon";
import { MatProgressSpinnerModule } from "@angular/material/progress-spinner";
import { MatToolbarModule } from "@angular/material/toolbar";
import { TranslatePipe, TranslateService } from "@ngx-translate/core";
import { Subject, takeUntil } from "rxjs";
import {
  addDays,
  daysBetween,
  endOfDay,
  PickedDate,
  startOfDay,
  toPickedDate,
} from "../../date-time";
import { ZacCheckbox } from "../../shared/form/checkbox/checkbox";
import { trackControl } from "../../shared/form/control-state";
import { ZacDate } from "../../shared/form/date/date";
import { ZacInput } from "../../shared/form/input/input";
import { injectMutation } from "../../shared/http/inject-mutation";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZakenService } from "../zaken.service";

@Component({
  templateUrl: "zaak-verlengen-dialog.component.html",
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatToolbarModule,
    MatDialogTitle,
    MatDialogContent,
    MatDividerModule,
    MatButtonModule,
    MatFormFieldModule,
    MatHint,
    MatIconModule,
    MatProgressSpinnerModule,
    TranslatePipe,
    ZacInput,
    ZacDate,
    ZacCheckbox,
  ],
})
export class ZaakVerlengenDialogComponent implements OnDestroy {
  private readonly formBuilder = inject(FormBuilder);
  private readonly translateService = inject(TranslateService);
  private readonly dialogRef =
    inject<MatDialogRef<ZaakVerlengenDialogComponent>>(MatDialogRef);
  private readonly zakenService = inject(ZakenService);
  protected readonly data = inject<{
    zaak: GeneratedType<"RestZaak">;
  }>(MAT_DIALOG_DATA);

  private readonly destroy$ = new Subject<void>();

  protected extendZaakMutation = injectMutation(
    () => this.zakenService.verlengenZaak(this.data.zaak.uuid),
    {
      onSuccess: (result) => {
        this.dialogRef.close(result);
      },
      onError: () => {
        this.dialogRef.disableClose = false;
      },
    },
  );

  protected readonly form = this.formBuilder.group({
    duurDagen: this.formBuilder.control<number | null>(null, [
      Validators.required,
      Validators.min(1),
      Validators.max(Number(this.data.zaak.zaaktype.verlengingstermijn)),
    ]),
    einddatumGepland: this.data.zaak.einddatumGepland
      ? this.formBuilder.control<PickedDate | null>(
          toPickedDate(this.data.zaak.einddatumGepland),
          [
            Validators.required,
            ...this.extensionBounds(
              this.data.zaak.einddatumGepland,
              startOfDay,
            ),
          ],
        )
      : this.formBuilder.control<PickedDate | null>(null),
    uiterlijkeEinddatumAfdoening: this.formBuilder.control<PickedDate | null>(
      toPickedDate(this.data.zaak.uiterlijkeEinddatumAfdoening),
      [
        Validators.required,
        ...this.extensionBounds(
          this.data.zaak.uiterlijkeEinddatumAfdoening,
          endOfDay,
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
  private readonly readForm = trackControl(signal(this.form));
  protected readonly formValid = this.readForm((form) => form.valid);
  protected readonly verlengduurHint: string;

  constructor() {
    const data = this.data;

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
          this.extensionDays(this.data.zaak.einddatumGepland, value),
        );
      });

    this.form.controls.uiterlijkeEinddatumAfdoening.valueChanges
      .pipe(takeUntil(this.destroy$))
      .subscribe((value) => {
        this.updateDateFields(
          this.extensionDays(
            this.data.zaak.uiterlijkeEinddatumAfdoening,
            value,
          ),
        );
      });
  }

  private extensionBounds(
    date: string | null,
    lastDayBound: (value: PickedDate) => PickedDate,
  ) {
    if (!date) return [];
    return [
      Validators.min(startOfDay(addDays(date, 1)).valueOf()),
      Validators.max(
        lastDayBound(
          addDays(date, this.data.zaak.zaaktype.verlengingstermijn ?? 0),
        ).valueOf(),
      ),
    ];
  }

  private extensionDays(
    date: string | null,
    value: PickedDate | null | undefined,
  ) {
    return date && value ? daysBetween(date, value) : Number.NaN;
  }

  private updateDateFields(duurDagen?: number | null) {
    this.form.reset(
      {
        duurDagen,
        einddatumGepland: this.data.zaak.einddatumGepland
          ? addDays(this.data.zaak.einddatumGepland, duurDagen ?? 0)
          : null,
        uiterlijkeEinddatumAfdoening: this.data.zaak
          .uiterlijkeEinddatumAfdoening
          ? addDays(this.data.zaak.uiterlijkeEinddatumAfdoening, duurDagen ?? 0)
          : null,
      },
      { emitEvent: false },
    );
    this.form.updateValueAndValidity();
  }

  protected verlengen() {
    this.dialogRef.disableClose = true;

    const formValues = this.form.value;

    this.extendZaakMutation.mutate({
      shouldTakenVerlengen: formValues.takenVerlengen ?? false,
      redenVerlenging: formValues.redenVerlenging,
      duurDagen: Number(formValues.duurDagen ?? 0),
    });
  }

  protected close() {
    this.dialogRef.close();
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
