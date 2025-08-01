/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, Inject, OnDestroy } from "@angular/core";
import { Validators } from "@angular/forms";
import { MAT_DIALOG_DATA, MatDialogRef } from "@angular/material/dialog";
import moment, { Moment } from "moment";
import { Subject, takeUntil } from "rxjs";
import { DateFormField } from "src/app/shared/material-form-builder/form-components/date/date-form-field";
import { DateFormFieldBuilder } from "src/app/shared/material-form-builder/form-components/date/date-form-field-builder";
import { HiddenFormField } from "src/app/shared/material-form-builder/form-components/hidden/hidden-form-field";
import { HiddenFormFieldBuilder } from "src/app/shared/material-form-builder/form-components/hidden/hidden-form-field-builder";
import { InputFormField } from "src/app/shared/material-form-builder/form-components/input/input-form-field";
import { InputFormFieldBuilder } from "src/app/shared/material-form-builder/form-components/input/input-form-field-builder";
import { AbstractFormField } from "../../shared/material-form-builder/model/abstract-form-field";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZakenService } from "../zaken.service";

@Component({
  templateUrl: "zaak-opschorten-dialog.component.html",
  styleUrls: ["./zaak-opschorten-dialog.component.less"],
})
export class ZaakOpschortenDialogComponent implements OnDestroy {
  formFields: AbstractFormField[][] = [];
  loading = true;

  duurDagenField: InputFormField<number>;
  einddatumGeplandField: DateFormField | HiddenFormField<string | Moment>;
  uiterlijkeEinddatumAfdoeningField: DateFormField;
  redenOpschortingField: InputFormField;

  private ngDestroy = new Subject();

  constructor(
    public dialogRef: MatDialogRef<ZaakOpschortenDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { zaak: GeneratedType<"RestZaak"> },
    private zakenService: ZakenService,
  ) {
    this.duurDagenField = new InputFormFieldBuilder<number>()
      .id("opschortduur")
      .label("opschortduur")
      .validators(Validators.required, Validators.min(1))
      .build();

    this.einddatumGeplandField = data.zaak.einddatumGepland
      ? new DateFormFieldBuilder(data.zaak.einddatumGepland)
          .id("einddatumGepland")
          .label("einddatumGepland")
          .readonly(!data.zaak.einddatumGepland)
          .validators(
            data.zaak.einddatumGepland
              ? Validators.required
              : Validators.nullValidator,
          )
          .build()
      : new HiddenFormFieldBuilder<Moment>().id("einddatumGepland").build();

    this.uiterlijkeEinddatumAfdoeningField = new DateFormFieldBuilder(
      data.zaak.uiterlijkeEinddatumAfdoening,
    )
      .id("uiterlijkeEinddatumAfdoening")
      .label("uiterlijkeEinddatumAfdoening")
      .validators(Validators.required)
      .build();

    this.redenOpschortingField = new InputFormFieldBuilder()
      .id("reden")
      .label("reden")
      .validators(Validators.required)
      .maxlength(200)
      .build();

    this.formFields = [
      [this.duurDagenField],
      [this.einddatumGeplandField],
      [this.uiterlijkeEinddatumAfdoeningField],
      [this.redenOpschortingField],
    ];

    this.duurDagenField.formControl.valueChanges
      .pipe(takeUntil(this.ngDestroy))
      .subscribe((value) => {
        let duur = Number(value);
        if (value == null || isNaN(duur)) {
          duur = 0;
        }
        this.updateDateFields(duur);
      });

    this.einddatumGeplandField.formControl.valueChanges
      // @ts-expect-error -- TODO TS2554: Expected 0 arguments, but got 1
      .pipe(takeUntil(this.ngDestroy))
      .subscribe((value) => {
        if (value == null) {
          this.resetFields();
        }
        this.updateDateFields(
          moment(value).diff(data.zaak.einddatumGepland, "days"),
        );
      });

    this.uiterlijkeEinddatumAfdoeningField.formControl.valueChanges
      .pipe(takeUntil(this.ngDestroy))
      .subscribe((value) => {
        if (value == null) {
          this.resetFields();
        }
        this.updateDateFields(
          moment(value).diff(data.zaak.uiterlijkeEinddatumAfdoening, "days"),
        );
      });

    this.dialogRef.afterOpened().subscribe(() => {
      this.loading = false;
    });
  }

  private updateDateFields(duur: number): void {
    if (duur <= 0) {
      this.resetFields();
      return;
    }

    this.duurDagenField.formControl.setValue(duur, { emitEvent: false });
    this.uiterlijkeEinddatumAfdoeningField.formControl.setValue(
      moment(this.data.zaak.uiterlijkeEinddatumAfdoening).add(duur, "days"),
      { emitEvent: false },
    );

    if (this.einddatumGeplandField.formControl.value === null) {
      return;
    }

    this.einddatumGeplandField.formControl.setValue(
      moment(this.data.zaak.einddatumGepland).add(duur, "days"),
      { emitEvent: false },
    );
  }

  private resetFields(): void {
    this.duurDagenField.formControl.setValue(null, { emitEvent: false });
    this.einddatumGeplandField.formControl.setValue(
      moment(this.data.zaak.einddatumGepland),
      { emitEvent: false },
    );

    this.uiterlijkeEinddatumAfdoeningField.formControl.setValue(
      moment(this.data.zaak.uiterlijkeEinddatumAfdoening),
      { emitEvent: false },
    );
  }

  opschorten(): void {
    this.dialogRef.disableClose = true;
    this.loading = true;

    const zaakOpschortGegevens: GeneratedType<"RESTZaakOpschortGegevens"> = {
      indicatieOpschorting: true,
      duurDagen: this.duurDagenField.formControl.value ?? undefined,
      einddatumGepland: moment(
        this.einddatumGeplandField.formControl.value,
      ).toISOString(),
      uiterlijkeEinddatumAfdoening: this.uiterlijkeEinddatumAfdoeningField
        .formControl.value
        ? moment(
            this.uiterlijkeEinddatumAfdoeningField.formControl.value,
          ).toISOString()
        : undefined,
      redenOpschorting: this.redenOpschortingField.formControl.value,
    };

    this.zakenService
      .opschortenZaak(this.data.zaak.uuid, zaakOpschortGegevens)
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

  disabled() {
    return (
      this.loading ||
      (this.data.zaak &&
        this.formFields.flat().some((field) => field.formControl.invalid))
    );
  }

  ngOnDestroy(): void {
    this.ngDestroy.next(null);
    this.ngDestroy.complete();
  }
}
