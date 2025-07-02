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

  duurDagenField: InputFormField<number>;
  einddatumGeplandField: DateFormField | HiddenFormField<string | Moment>;
  uiterlijkeEinddatumAfdoeningField: DateFormField;
  redenVerlengingField: InputFormField;
  takenVerlengenField: CheckboxFormField;

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
    @Inject(MAT_DIALOG_DATA) public data: { zaak: Zaak },
  ) {
    this.verlengingstermijn = this.data.zaak.zaaktype.verlengingstermijn;
    this.verlengduurHint = this.translateService.instant(
      `hint.zaak.dialoog.verlengen.verlengduur${Number(this.data.zaak.zaaktype.verlengingstermijn) > 1 ? ".meervoud" : ""}`,
      {
        verlengDuur: data.zaak.zaaktype.verlengingstermijn,
      },
    );

    const maxDateEinddatumGepland = moment(this.data.zaak.einddatumGepland)
      .add(this.data.zaak.zaaktype.verlengingstermijn, "days")
      .toDate();

    this.form.controls.duurDagen.setValidators([
      Validators.required,
      Validators.min(1),
      Validators.max(Number(this.data.zaak.zaaktype.verlengingstermijn)),
    ]);

    console.log("data.zaak.einddatumGepland", data.zaak.einddatumGepland);
    console.log(
      "data.zaak.uiterlijkeEinddatumAfdoening",
      data.zaak.uiterlijkeEinddatumAfdoening,
    );

    if (this.data.zaak.einddatumGepland) {
      this.form.controls.einddatumGepland.setValue(
        moment(this.data.zaak.einddatumGepland),
      );
      this.form.controls.einddatumGepland.setValidators([
        Validators.required,
        Validators.min(1),
      ]);
    }

    this.form.controls.uiterlijkeEinddatumAfdoening.setValue(
      moment(this.data.zaak.uiterlijkeEinddatumAfdoening),
    );
    this.form.controls.uiterlijkeEinddatumAfdoening.setValidators([
      Validators.required,
      Validators.min(1),
    ]);

    this.form.controls.duurDagen.valueChanges
      .pipe(takeUntil(this.ngDestroy))
      .subscribe((value) => {
        let duur = Number(value);
        if (value == null || isNaN(duur)) {
          duur = 0;
        }
        this.updateDateFieldsNNNN(duur);
      });

    this.form.controls.uiterlijkeEinddatumAfdoening.valueChanges
      .pipe(takeUntil(this.ngDestroy))
      .subscribe((value) => {
        if (value == null) {
          this.resetFieldsNNNNN;
        }
        this.updateDateFieldsNNNN(
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
          this.resetFieldsNNNNN;
        }
        this.updateDateFieldsNNNN(
          moment(value).diff(this.data.zaak.einddatumGepland, "days"),
        );
      });

    //
    //
    //
    //
    //
    //
    //
    //
    //
    //
    this.duurDagenField = new InputFormFieldBuilder<number>()
      .id("verlengduur")
      .label("verlengduur")
      .validators(
        Validators.required,
        Validators.min(1),
        Validators.max(Number(this.data.zaak.zaaktype.verlengingstermijn)),
      )
      .hint(
        this.translateService.instant(
          `hint.zaak.dialoog.verlengen.verlengduur${Number(this.data.zaak.zaaktype.verlengingstermijn) > 1 ? ".meervoud" : ""}`,
          {
            verlengDuur: data.zaak.zaaktype.verlengingstermijn,
          },
        ),
      )
      .styleClass("form-field-hint")
      .build();

    // const maxDateEinddatumGepland = moment(this.data.zaak.einddatumGepland)
    //   .add(this.data.zaak.zaaktype.verlengingstermijn, "days")
    //   .toDate();

    this.einddatumGeplandField = data.zaak?.einddatumGepland
      ? new DateFormFieldBuilder(this.data.zaak.einddatumGepland)
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
      : new HiddenFormFieldBuilder<string>().id("einddatumGepland").build();

    const maxDateUiterlijkeEinddatumAfdoening = moment(
      data.zaak.uiterlijkeEinddatumAfdoening,
    )
      .add(this.data.zaak.zaaktype.verlengingstermijn, "days")
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
      .maxlength(200)
      .build();

    this.takenVerlengenField = new CheckboxFormFieldBuilder(false)
      .id("taken.verlengen")
      .label("taken.verlengen")
      .build();

    this.formFields = [
      [this.duurDagenField],
      [this.einddatumGeplandField],
      [this.uiterlijkeEinddatumAfdoeningField],
      [this.takenVerlengenField],
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
      // @ts-expect-error -- TODO TS2554: Expected 0 arguments, but got 1
      .pipe(takeUntil(this.ngDestroy))
      .subscribe((value) => {
        if (value == null) {
          this.resetFields();
        }
        this.updateDateFields(
          moment(value).diff(this.data.zaak.einddatumGepland, "days"),
        );
      });

    this.uiterlijkeEinddatumAfdoeningField.formControl.valueChanges
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

  private updateDateFieldsNNNN(duur: number): void {
    if (duur <= 0) {
      this.resetFieldsNNNNN();
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

  private resetFieldsNNNNN(): void {
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

    const zaakVerlengGegevens: GeneratedType<"RESTZaakVerlengGegevens"> = {
      duurDagen: this.duurDagenField.formControl.value ?? undefined,
      einddatumGepland: this.einddatumGeplandField.formControl.value
        ? moment(this.einddatumGeplandField.formControl.value).toISOString()
        : undefined,
      uiterlijkeEinddatumAfdoening: this.uiterlijkeEinddatumAfdoeningField
        .formControl.value
        ? moment(
            this.uiterlijkeEinddatumAfdoeningField.formControl.value,
          ).toISOString()
        : undefined,
      redenVerlenging: this.redenVerlengingField.formControl.value,
      takenVerlengen: Boolean(this.takenVerlengenField.formControl.value),
    };

    console.log(
      "this.form.controls.takenVerlengen.value",
      this.form.controls.takenVerlengen.value,
      typeof this.form.controls.takenVerlengen.value,
    );

    const zaakVerlengGegevensNNNNNN: GeneratedType<"RESTZaakVerlengGegevens"> =
      {
        duurDagen: Number(this.form.controls.duurDagen.value) ?? undefined,
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
      };

    console.log("zaakVerlengGegevens", zaakVerlengGegevens);
    console.log("zaakVerlengGegevensNNNNNN", zaakVerlengGegevensNNNNNN);

    return;

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
    return this.loading || (this.data.zaak && !this.form.valid);
  }

  ngOnDestroy(): void {
    this.ngDestroy.next();
    this.ngDestroy.complete();
  }
}
