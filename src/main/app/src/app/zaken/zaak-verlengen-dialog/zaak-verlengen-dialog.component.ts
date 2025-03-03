/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, Inject, OnDestroy } from "@angular/core";
import { Validators } from "@angular/forms";
import { MAT_DIALOG_DATA, MatDialogRef } from "@angular/material/dialog";
import { TranslateService } from "@ngx-translate/core";
import moment from "moment";
import { Subject, takeUntil } from "rxjs";
import { CheckboxFormField } from "src/app/shared/material-form-builder/form-components/checkbox/checkbox-form-field";
import { CheckboxFormFieldBuilder } from "src/app/shared/material-form-builder/form-components/checkbox/checkbox-form-field-builder";
import { DateFormField } from "src/app/shared/material-form-builder/form-components/date/date-form-field";
import { DateFormFieldBuilder } from "src/app/shared/material-form-builder/form-components/date/date-form-field-builder";
import { HiddenFormField } from "src/app/shared/material-form-builder/form-components/hidden/hidden-form-field";
import { HiddenFormFieldBuilder } from "src/app/shared/material-form-builder/form-components/hidden/hidden-form-field-builder";
import { InputFormField } from "src/app/shared/material-form-builder/form-components/input/input-form-field";
import { InputFormFieldBuilder } from "src/app/shared/material-form-builder/form-components/input/input-form-field-builder";
import { AbstractFormField } from "../../shared/material-form-builder/model/abstract-form-field";
import { GeneratedType } from "../../shared/utils/generated-types";
import { Zaak } from "../model/zaak";
import { ZakenService } from "../zaken.service";

@Component({
  templateUrl: "zaak-verlengen-dialog.component.html",
  styleUrls: ["./zaak-verlengen-dialog.component.less"],
})
export class ZaakVerlengenDialogComponent implements OnDestroy {
  formFields: AbstractFormField[][] = [];
  loading = true;

  duurDagenField: InputFormField;
  einddatumGeplandField: DateFormField | HiddenFormField;
  uiterlijkeEinddatumAfdoeningField: DateFormField;
  redenVerlengingField: InputFormField;
  takenVerlengenField: CheckboxFormField;

  private ngDestroy = new Subject<void>();

  constructor(
    private translateService: TranslateService,
    public dialogRef: MatDialogRef<ZaakVerlengenDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { zaak: Zaak },
    private zakenService: ZakenService,
  ) {
    this.duurDagenField = new InputFormFieldBuilder()
      .id("verlengduur")
      .label("verlengduur")
      .validators(
        Validators.required,
        Validators.min(1),
        Validators.max(data.zaak.zaaktype.verlengingstermijn),
      )
      .hint(
        this.translateService.instant(
          `hint.zaak.dialoog.verlengen.verlengduur${data.zaak.zaaktype.verlengingstermijn > 1 ? ".meervoud" : ""}`,
          {
            verlengDuur: data.zaak.zaaktype.verlengingstermijn,
          },
        ),
      )
      .styleClass("form-field-hint")
      .build();

    const maxDateEinddatumGepland = moment(data.zaak.einddatumGepland)
      .add(data.zaak.zaaktype.verlengingstermijn, "days")
      .toDate();

    this.einddatumGeplandField = data.zaak?.einddatumGepland
      ? new DateFormFieldBuilder(data.zaak.einddatumGepland)
          .id("einddatumGepland")
          .label("einddatumGepland")
          .readonly(!data.zaak.einddatumGepland)
          .validators(
            data.zaak.einddatumGepland
              ? Validators.required
              : Validators.nullValidator,
          )
          .maxDate(maxDateEinddatumGepland)
          .build()
      : new HiddenFormFieldBuilder().id("einddatumGepland").build();

    const maxDateUiterlijkeEinddatumAfdoening = moment(
      data.zaak.uiterlijkeEinddatumAfdoening,
    )
      .add(data.zaak.zaaktype.verlengingstermijn, "days")
      .toDate();

    this.uiterlijkeEinddatumAfdoeningField = new DateFormFieldBuilder(
      data.zaak.uiterlijkeEinddatumAfdoening,
    )
      .id("uiterlijkeEinddatumAfdoening")
      .label("uiterlijkeEinddatumAfdoening")
      .validators(Validators.required)
      .maxDate(maxDateUiterlijkeEinddatumAfdoening)
      .build();

    this.redenVerlengingField = new InputFormFieldBuilder()
      .id("reden")
      .label("reden")
      .validators(Validators.required)
      .build();

    this.takenVerlengenField = new CheckboxFormFieldBuilder(false)
      .id("taken.verlengen")
      .label("taken.verlengen")
      .build();

    this.formFields = [
      [this.duurDagenField],
      [this.einddatumGeplandField],
      [this.uiterlijkeEinddatumAfdoeningField],
      [this.redenVerlengingField],
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

    this.duurDagenField?.formControl.setValue(duur, { emitEvent: false });
    this.uiterlijkeEinddatumAfdoeningField?.formControl.setValue(
      moment(this.data.zaak.uiterlijkeEinddatumAfdoening).add(duur, "days"),
      { emitEvent: false },
    );

    if (this.einddatumGeplandField?.formControl.value === null) {
      return;
    }

    this.einddatumGeplandField?.formControl.setValue(
      moment(this.data.zaak.einddatumGepland).add(duur, "days"),
      { emitEvent: false },
    );
  }

  private resetFields(): void {
    this.duurDagenField.formControl.setValue(null, { emitEvent: false });
    this.einddatumGeplandField?.formControl.setValue(
      moment(this.data.zaak.einddatumGepland),
      { emitEvent: false },
    );

    this.uiterlijkeEinddatumAfdoeningField.formControl.setValue(
      moment(this.data.zaak.uiterlijkeEinddatumAfdoening),
      { emitEvent: false },
    );
  }

  verlengen(): void {
    this.dialogRef.disableClose = true;
    this.loading = true;

    const zaakVerlengGegevens: GeneratedType<"RESTZaakVerlengGegevens"> = {
      duurDagen: this.duurDagenField.formControl.value,
      einddatumGepland: this.einddatumGeplandField.formControl.value,
      uiterlijkeEinddatumAfdoening:
        this.uiterlijkeEinddatumAfdoeningField.formControl.value,
      redenVerlenging: this.redenVerlengingField.formControl.value,
      takenVerlengen: this.takenVerlengenField.formControl.value,
    };

    this.zakenService
      .verlengenZaak(this.data.zaak.uuid, zaakVerlengGegevens)
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
    this.ngDestroy.next();
    this.ngDestroy.complete();
  }
}
