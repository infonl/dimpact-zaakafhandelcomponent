/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, Inject, OnInit } from "@angular/core";
import { FormControl } from "@angular/forms";
import { MAT_DIALOG_DATA, MatDialogRef } from "@angular/material/dialog";
import { Observable, of } from "rxjs";
import { map, startWith } from "rxjs/operators";
import { UtilService } from "../../core/service/util.service";
import { GeneratedType } from "../../shared/utils/generated-types";
import { GebruikersvoorkeurenService } from "../gebruikersvoorkeuren.service";

@Component({
  templateUrl: "./zoekopdracht-save-dialog.component.html",
  styleUrls: ["./zoekopdracht-save-dialog.component.less"],
})
export class ZoekopdrachtSaveDialogComponent implements OnInit {
  loading = false;
  formControl = new FormControl("");
  filteredOptions: Observable<string[]> = of([]);

  constructor(
    public readonly dialogRef: MatDialogRef<ZoekopdrachtSaveDialogComponent>,
    @Inject(MAT_DIALOG_DATA)
    public readonly data: {
      zoekopdrachten: GeneratedType<"RESTZoekopdracht">[];
      lijstID: GeneratedType<"Werklijst">;
      zoekopdracht: unknown;
    },
    private readonly gebruikersvoorkeurenService: GebruikersvoorkeurenService,
    private readonly utilService: UtilService,
  ) {}

  close() {
    this.dialogRef.close();
  }

  opslaan() {
    this.dialogRef.disableClose = true;
    this.loading = true;
    const zoekopdracht = this.isNew()
      ? {
          naam: this.formControl.value,
          json: JSON.stringify(this.data.zoekopdracht),
          lijstID: this.data.lijstID,
        }
      : {
          ...this.readZoekopdracht(),
          json: JSON.stringify(this.data.zoekopdracht),
        };
    this.gebruikersvoorkeurenService
      .createOrUpdateZoekOpdrachten(
        zoekopdracht as GeneratedType<"RESTZoekopdracht">,
      )
      .subscribe({
        next: () => {
          this.utilService.openSnackbar("msg.zoekopdracht.opgeslagen");
          this.dialogRef.close(true);
        },
        error: () => this.dialogRef.close(),
      });
  }

  isNew() {
    return (
      this.data.zoekopdrachten.filter(
        (value) =>
          value.naam?.toLowerCase() ===
          this.formControl.value?.toLowerCase().trim(),
      ).length === 0
    );
  }

  readZoekopdracht() {
    return this.data.zoekopdrachten.filter(
      (value) =>
        value.naam?.toLowerCase() ===
        this.formControl.value?.toLowerCase().trim(),
    )[0];
  }

  ngOnInit(): void {
    this.filteredOptions = this.formControl.valueChanges.pipe(
      startWith(""),
      map((value) => this._filter(value || "")),
    );
  }

  private _filter(name: string) {
    const filterValue = name.toLowerCase();
    return this.data.zoekopdrachten
      .map((x) => x.naam ?? "")
      .filter((option) => option?.toLowerCase().includes(filterValue));
  }
}
