/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { AsyncPipe, NgFor } from "@angular/common";
import { Component, inject, OnInit } from "@angular/core";
import { FormControl, ReactiveFormsModule } from "@angular/forms";
import { MatAutocompleteModule } from "@angular/material/autocomplete";
import { MatButtonModule } from "@angular/material/button";
import {
  MAT_DIALOG_DATA,
  MatDialogModule,
  MatDialogRef,
} from "@angular/material/dialog";
import { MatDividerModule } from "@angular/material/divider";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatIconModule } from "@angular/material/icon";
import { MatInputModule } from "@angular/material/input";
import { MatProgressSpinnerModule } from "@angular/material/progress-spinner";
import { MatToolbarModule } from "@angular/material/toolbar";
import { TranslateModule } from "@ngx-translate/core";
import { Observable, of } from "rxjs";
import { map, startWith } from "rxjs/operators";
import { UtilService } from "../../core/service/util.service";
import { GeneratedType } from "../../shared/utils/generated-types";
import { GebruikersvoorkeurenService } from "../gebruikersvoorkeuren.service";

@Component({
  templateUrl: "./zoekopdracht-save-dialog.component.html",
  styleUrls: ["./zoekopdracht-save-dialog.component.less"],
  standalone: true,
  imports: [
    AsyncPipe,
    NgFor,
    ReactiveFormsModule,
    MatAutocompleteModule,
    MatButtonModule,
    MatDialogModule,
    MatDividerModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatToolbarModule,
    TranslateModule,
  ],
})
export class ZoekopdrachtSaveDialogComponent implements OnInit {
  private readonly dialogRef = inject(
    MatDialogRef<ZoekopdrachtSaveDialogComponent>,
  );
  protected readonly data = inject<{
    zoekopdrachten: GeneratedType<"RESTZoekopdracht">[];
    lijstID: GeneratedType<"Werklijst">;
    zoekopdracht: unknown;
  }>(MAT_DIALOG_DATA);
  private readonly gebruikersvoorkeurenService = inject(
    GebruikersvoorkeurenService,
  );
  private readonly utilService = inject(UtilService);

  protected loading = false;
  protected formControl = new FormControl("");
  protected filteredOptions: Observable<string[]> = of([]);

  protected close() {
    this.dialogRef.close();
  }

  protected opslaan() {
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

  protected isNew() {
    return (
      this.data.zoekopdrachten.filter(
        (value) =>
          value.naam?.toLowerCase() ===
          this.formControl.value?.toLowerCase().trim(),
      ).length === 0
    );
  }

  private readZoekopdracht() {
    return this.data.zoekopdrachten.filter(
      (value) =>
        value.naam?.toLowerCase() ===
        this.formControl.value?.toLowerCase().trim(),
    )[0];
  }

  ngOnInit() {
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
