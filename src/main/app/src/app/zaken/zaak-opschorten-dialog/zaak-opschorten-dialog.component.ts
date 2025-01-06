/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, Inject, OnInit } from "@angular/core";
import { Validators } from "@angular/forms";
import { MAT_DIALOG_DATA, MatDialogRef } from "@angular/material/dialog";
import { AbstractFormField } from "../../shared/material-form-builder/model/abstract-form-field";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZakenService } from "../zaken.service";
import { InputFormFieldBuilder } from "src/app/shared/material-form-builder/form-components/input/input-form-field-builder";
import { DateFormFieldBuilder } from "src/app/shared/material-form-builder/form-components/date/date-form-field-builder";
import { Zaak } from "../model/zaak";
import { HiddenFormFieldBuilder } from "src/app/shared/material-form-builder/form-components/hidden/hidden-form-field-builder";
import { InputFormField } from "src/app/shared/material-form-builder/form-components/input/input-form-field";
import { DateFormField } from "src/app/shared/material-form-builder/form-components/date/date-form-field";
import { HiddenFormField } from "src/app/shared/material-form-builder/form-components/hidden/hidden-form-field";
import moment from "moment";
import { Subject, takeUntil } from "rxjs";

@Component({
  templateUrl: "zaak-opschorten-dialog.component.html",
  styleUrls: ["./zaak-opschorten-dialog.component.less"],
})
export class ZaakOpschortenDialogComponent implements OnInit {
  formFields: AbstractFormField[];
  zaak: Zaak;
  loading: boolean = true;

  duurField: InputFormField;
  einddatumGeplandField: DateFormField | HiddenFormField;
  uiterlijkeEinddatumAfdoeningField: DateFormField;
  opschortReden: InputFormField;

  private ngDestroy = new Subject<void>();

  constructor(
    public dialogRef: MatDialogRef<ZaakOpschortenDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { zaak: Zaak },
    private zakenService: ZakenService,
  ) {}

  ngOnInit(): void {
    console.log("zaak", this.data.zaak);
    this.initFormFields();

    this.dialogRef.afterOpened().subscribe(() => {
      this.loading = false;
    });
  }

  initFormFields(): void {
    this.duurField = new InputFormFieldBuilder()
      .id("opschortduur")
      .label("opschortduur")
      .validators(Validators.required, Validators.min(1))
      .build();

    this.einddatumGeplandField = this.data.zaak?.einddatumGepland
      ? new DateFormFieldBuilder(this.data.zaak.einddatumGepland)
          .id("einddatumGepland")
          .label("einddatumGepland")
          .readonly(!this.data.zaak.einddatumGepland)
          .validators(
            this.data.zaak.einddatumGepland
              ? Validators.required
              : Validators.nullValidator,
          )
          .build()
      : new HiddenFormFieldBuilder().id("einddatumGepland").build();

    this.uiterlijkeEinddatumAfdoeningField = new DateFormFieldBuilder(
      this.data.zaak.uiterlijkeEinddatumAfdoening,
    )
      .id("uiterlijkeEinddatumAfdoening")
      .label("uiterlijkeEinddatumAfdoening")
      .validators(Validators.required)
      .build();

    this.opschortReden = new InputFormFieldBuilder()
      .id("reden")
      .label("reden")
      .validators(Validators.required)
      .build();

    this.formFields = [
      this.duurField,
      this.einddatumGeplandField,
      this.uiterlijkeEinddatumAfdoeningField,
      this.opschortReden,
    ];

    this.duurField.formControl.valueChanges
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
  }

  private updateDateFields(duur: number): void {
    if (duur > 0) {
      this.duurField.formControl.setValue(duur, { emitEvent: false });
      if (this.einddatumGeplandField.formControl.value != null) {
        this.einddatumGeplandField.formControl.setValue(
          moment(this.data.zaak.einddatumGepland).add(duur, "days"),
          { emitEvent: false },
        );
      }
      this.uiterlijkeEinddatumAfdoeningField.formControl.setValue(
        moment(this.data.zaak.uiterlijkeEinddatumAfdoening).add(duur, "days"),
        { emitEvent: false },
      );
    } else {
      this.resetFields();
    }
  }

  private resetFields(): void {
    this.duurField.formControl.setValue(null, { emitEvent: false });
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

    const zaakOpschortGegevens: GeneratedType<"RESTZaakOpschortGegevens"> = {};
    zaakOpschortGegevens.indicatieOpschorting = true;
    zaakOpschortGegevens.duurDagen = this.duurField.formControl.value;
    zaakOpschortGegevens.einddatumGepland =
      this.einddatumGeplandField.formControl.value;
    zaakOpschortGegevens.uiterlijkeEinddatumAfdoening =
      this.uiterlijkeEinddatumAfdoeningField.formControl.value;
    zaakOpschortGegevens.redenOpschorting =
      this.opschortReden.formControl.value;

    this.zakenService
      .opschortenZaak(this.data.zaak.uuid, zaakOpschortGegevens)
      .subscribe({
        next: () => {
          this.loading = false;
          this.dialogRef.close(true);
          this.zakenService.readOpschortingZaak(this.data.zaak.uuid);
        },
        error: (err) => {
          console.error("Error while suspending case:", err);
          this.loading = false;
          this.dialogRef.disableClose = false;
        },
      });

    this.dialogRef.close(true);
  }

  close(): void {
    this.dialogRef.close();
  }

  disabled() {
    return (
      this.loading ||
      (this.data.zaak &&
        this.formFields.some((field) => field.formControl.invalid))
    );
  }

  ngOnDestroy(): void {
    this.ngDestroy.next();
    this.ngDestroy.complete();
  }
}
