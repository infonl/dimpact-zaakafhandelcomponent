/*
 * SPDX-FileCopyrightText: 2021 - 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, Inject, OnInit } from "@angular/core";
import { MAT_DIALOG_DATA, MatDialogRef } from "@angular/material/dialog";
import { Group } from "../../identity/model/group";
import { User } from "../../identity/model/user";
import { InputFormField } from "../../shared/material-form-builder/form-components/input/input-form-field";
import { InputFormFieldBuilder } from "../../shared/material-form-builder/form-components/input/input-form-field-builder";
import { MedewerkerGroepFieldBuilder } from "../../shared/material-form-builder/form-components/medewerker-groep/medewerker-groep-field-builder";
import { MedewerkerGroepFormField } from "../../shared/material-form-builder/form-components/medewerker-groep/medewerker-groep-form-field";
import { MaterialFormBuilderService } from "../../shared/material-form-builder/material-form-builder.service";
import { TaakZoekObject } from "../../zoeken/model/taken/taak-zoek-object";
import { TakenService } from "../taken.service";

@Component({
  selector: "zac-taken-verdelen-dialog",
  templateUrl: "./taken-verdelen-dialog.component.html",
  styleUrls: ["./taken-verdelen-dialog.component.less"],
})
export class TakenVerdelenDialogComponent implements OnInit {
  medewerkerGroepFormField: MedewerkerGroepFormField;
  redenFormField: InputFormField;
  loading = false;

  constructor(
    public dialogRef: MatDialogRef<TakenVerdelenDialogComponent>,
    @Inject(MAT_DIALOG_DATA)
    public data: {
      taken: TaakZoekObject[];
      screenEventResourceId: string;
    },
    private mfbService: MaterialFormBuilderService,
    private takenService: TakenService,
  ) {}

  close(): void {
    this.dialogRef.close(false);
  }

  ngOnInit(): void {
    this.medewerkerGroepFormField = new MedewerkerGroepFieldBuilder()
      .id("toekenning")
      .groepLabel("actie.taak.toekennen.groep")
      .medewerkerLabel("actie.taak.toekennen.medewerker")
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
    const toekenning: { groep?: Group; medewerker?: User } =
      this.medewerkerGroepFormField.formControl.value;
    const reden: string = this.redenFormField.formControl.value;
    this.dialogRef.disableClose = true;
    this.loading = true;
    this.takenService
      .verdelenVanuitLijst(
        this.data.taken,
        reden,
        this.data.screenEventResourceId,
        toekenning.groep,
        toekenning.medewerker,
      )
      .subscribe(() => {
        this.dialogRef.close(toekenning);
      });
  }
}
