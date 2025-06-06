/*
 * SPDX-FileCopyrightText: 2021 - 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, Inject, OnInit } from "@angular/core";
import { MAT_DIALOG_DATA, MatDialogRef } from "@angular/material/dialog";
import { InputFormField } from "../../shared/material-form-builder/form-components/input/input-form-field";
import { InputFormFieldBuilder } from "../../shared/material-form-builder/form-components/input/input-form-field-builder";
import { MedewerkerGroepFieldBuilder } from "../../shared/material-form-builder/form-components/medewerker-groep/medewerker-groep-field-builder";
import { MedewerkerGroepFormField } from "../../shared/material-form-builder/form-components/medewerker-groep/medewerker-groep-form-field";
import { MaterialFormBuilderService } from "../../shared/material-form-builder/material-form-builder.service";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZaakZoekObject } from "../../zoeken/model/zaken/zaak-zoek-object";
import { ZakenService } from "../zaken.service";

@Component({
  templateUrl: "zaken-verdelen-dialog.component.html",
  styleUrls: ["./zaken-verdelen-dialog.component.less"],
})
export class ZakenVerdelenDialogComponent implements OnInit {
  medewerkerGroepFormField: MedewerkerGroepFormField;
  redenFormField: InputFormField;
  loading: boolean;

  constructor(
    public dialogRef: MatDialogRef<ZakenVerdelenDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: ZaakZoekObject[],
    private mfbService: MaterialFormBuilderService,
    private zakenService: ZakenService,
  ) {}

  close(): void {
    this.dialogRef.close(false);
  }

  ngOnInit(): void {
    this.medewerkerGroepFormField = new MedewerkerGroepFieldBuilder()
      .id("toekenning")
      .groepLabel("actie.zaak.toekennen.groep")
      .medewerkerLabel("actie.zaak.toekennen.medewerker")
      .build();
    this.redenFormField = new InputFormFieldBuilder()
      .id("reden")
      .label("reden")
      .maxlength(100)
      .build();
  }

  isDisabled(): boolean {
    return (
      (!this.medewerkerGroepFormField.medewerker.value &&
        !this.medewerkerGroepFormField.groep.value) ||
      this.medewerkerGroepFormField.formControl.invalid ||
      this.loading
    );
  }

  verdeel(): void {
    this.redenFormField.readonly = true;
    const toekenning: {
      groep?: GeneratedType<"RestGroup">;
      medewerker?: GeneratedType<"RestUser">;
    } = this.medewerkerGroepFormField.formControl.value;
    this.dialogRef.disableClose = true;
    this.loading = true;
    this.zakenService
      .verdelenVanuitLijst(
        this.data.map((zaak) => zaak.id),
        crypto.randomUUID(),
        toekenning.groep,
        toekenning.medewerker,
        this.redenFormField.formControl.value,
      )
      .subscribe(() => {
        this.dialogRef.close(toekenning);
      });
  }
}
