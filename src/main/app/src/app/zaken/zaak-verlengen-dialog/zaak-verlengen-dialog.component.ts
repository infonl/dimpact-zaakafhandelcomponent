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
import { AbstractFormField } from "../../shared/material-form-builder/model/abstract-form-field";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZakenService } from "../zaken.service";

@Component({
  templateUrl: "zaak-verlengen-dialog.component.html",
  styleUrls: ["./zaak-verlengen-dialog.component.less"],
})
export class ZaakVerlengenDialogComponent implements OnDestroy {
  formFields: AbstractFormField[][] = [];
  loading = false;

  private ngDestroy = new Subject<void>();

  protected readonly form = this.formBuilder.group({
    duurDagen: this.formBuilder.control("", []),
    einddatumGepland: this.formBuilder.control<Moment | null>(null),
    uiterlijkeEinddatumAfdoening: this.formBuilder.control<Moment | null>(null),
    verlengingVastleggen: this.formBuilder.control<boolean>(false, []),
    redenVerlenging: this.formBuilder.control("", [
      Validators.required,
      Validators.maxLength(200),
    ]),
    takenVerlengen: this.formBuilder.control("", []),
  });
  protected verlengduurHint: string;
  protected verlengingstermijn: number | null | undefined;

  constructor(
    private readonly formBuilder: FormBuilder,
    private translateService: TranslateService,
    public dialogRef: MatDialogRef<ZaakVerlengenDialogComponent>,
    private zakenService: ZakenService,
    @Inject(MAT_DIALOG_DATA) public data: { zaak: GeneratedType<"RestZaak"> },
  ) {
    this.verlengingstermijn = this.data.zaak.zaaktype.verlengingstermijn;
    this.verlengduurHint = this.translateService.instant(
      `hint.zaak.dialoog.verlengen.verlengduur${Number(this.data.zaak.zaaktype.verlengingstermijn) > 1 ? ".meervoud" : ""}`,
      {
        verlengDuur: data.zaak.zaaktype.verlengingstermijn,
      },
    );

    this.form.controls.duurDagen.setValidators([
      Validators.required,
      Validators.min(1),
      Validators.max(Number(this.data.zaak.zaaktype.verlengingstermijn)),
    ]);

    if (this.data.zaak.einddatumGepland) {
      this.form.controls.einddatumGepland.setValue(
        moment(this.data.zaak.einddatumGepland),
      );
      this.form.controls.einddatumGepland.setValidators([
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
            .endOf("day")
            .valueOf(),
        ),
      ]);
    }

    this.form.controls.uiterlijkeEinddatumAfdoening.setValue(
      moment(this.data.zaak.uiterlijkeEinddatumAfdoening),
    );
    this.form.controls.uiterlijkeEinddatumAfdoening.setValidators([
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
    ]);

    this.form.controls.duurDagen.valueChanges
      .pipe(takeUntil(this.ngDestroy))
      .subscribe((value) => {
        let duur = Number(value);
        if (value == null || isNaN(duur)) {
          duur = 0;
        }
        this.updateDateFields(duur);
      });

    this.form.controls.uiterlijkeEinddatumAfdoening.valueChanges
      .pipe(takeUntil(this.ngDestroy))
      .subscribe((value) => {
        if (value == null) {
          this.resetFields();
        }
        this.updateDateFields(
          moment(value).diff(
            this.data.zaak.uiterlijkeEinddatumAfdoening,
            "days",
          ),
        );
      });

    this.form.controls.einddatumGepland.valueChanges
      .pipe(takeUntil(this.ngDestroy))
      .subscribe((value) => {
        if (value == null) {
          this.resetFields();
        }
        this.updateDateFields(
          moment(value).diff(this.data.zaak.einddatumGepland, "days"),
        );
      });
  }

  private updateDateFields(duur: number): void {
    if (duur <= 0) {
      this.resetFields();
      return;
    }

    this.form.controls.duurDagen.setValue(duur.toString(), {
      emitEvent: false,
    });

    this.form.controls.uiterlijkeEinddatumAfdoening.setValue(
      moment(this.data.zaak.uiterlijkeEinddatumAfdoening).add(duur, "days"),
      { emitEvent: false },
    );

    if (!this.data.zaak.einddatumGepland) {
      return;
    }

    this.form.controls.einddatumGepland.setValue(
      moment(this.data.zaak.einddatumGepland).add(duur, "days"),
      { emitEvent: false },
    );
  }

  private resetFields(): void {
    this.form.controls.duurDagen.setValue("", { emitEvent: false });

    this.form.controls.uiterlijkeEinddatumAfdoening.setValue(
      moment(this.data.zaak.uiterlijkeEinddatumAfdoening),
      { emitEvent: false },
    );

    if (!this.data.zaak.einddatumGepland) {
      return;
    }

    this.form.controls.einddatumGepland.setValue(
      moment(this.data.zaak.einddatumGepland),
      { emitEvent: false },
    );
  }

  verlengen(): void {
    this.dialogRef.disableClose = true;
    this.loading = true;

    this.zakenService
      .verlengenZaak(this.data.zaak.uuid, {
        duurDagen: Number(this.form.controls.duurDagen.value),
        einddatumGepland: this.form.controls.einddatumGepland.value
          ? moment(this.form.controls.einddatumGepland.value).toISOString()
          : undefined,
        uiterlijkeEinddatumAfdoening: this.form.controls
          .uiterlijkeEinddatumAfdoening.value
          ? moment(
              this.form.controls.uiterlijkeEinddatumAfdoening.value,
            ).toISOString()
          : undefined,
        redenVerlenging: this.form.controls.redenVerlenging.value,
        takenVerlengen: Boolean(this.form.controls.takenVerlengen.value),
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

  close(): void {
    this.dialogRef.close();
  }

  ngOnDestroy(): void {
    this.ngDestroy.next();
    this.ngDestroy.complete();
  }
}
