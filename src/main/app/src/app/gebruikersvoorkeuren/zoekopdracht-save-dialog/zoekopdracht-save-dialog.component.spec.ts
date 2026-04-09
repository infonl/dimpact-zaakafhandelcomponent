/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { AsyncPipe, NgFor, NgIf } from "@angular/common";
import { provideHttpClient } from "@angular/common/http";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { ReactiveFormsModule } from "@angular/forms";
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
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { provideRouter } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { of } from "rxjs";
import { GeneratedType } from "../../shared/utils/generated-types";
import { GebruikersvoorkeurenService } from "../gebruikersvoorkeuren.service";
import { ZoekopdrachtSaveDialogComponent } from "./zoekopdracht-save-dialog.component";

const makeZoekopdracht = (naam: string): GeneratedType<"RESTZoekopdracht"> => ({
  naam,
  json: "{}",
  lijstID: "TAKEN_WERKVOORRAAD" as GeneratedType<"Werklijst">,
});

const setup = (zoekopdrachten: GeneratedType<"RESTZoekopdracht">[] = []) => {
  const dialogRefMock = { close: jest.fn(), disableClose: false };
  TestBed.configureTestingModule({
    imports: [
      ZoekopdrachtSaveDialogComponent,
      NoopAnimationsModule,
      TranslateModule.forRoot(),
      ReactiveFormsModule,
      NgFor,
      NgIf,
      AsyncPipe,
      MatToolbarModule,
      MatDialogModule,
      MatDividerModule,
      MatIconModule,
      MatButtonModule,
      MatProgressSpinnerModule,
      MatFormFieldModule,
      MatInputModule,
      MatAutocompleteModule,
    ],
    providers: [
      provideHttpClient(),
      provideRouter([]),
      {
        provide: MAT_DIALOG_DATA,
        useValue: {
          zoekopdrachten,
          lijstID: "TAKEN_WERKVOORRAAD" as GeneratedType<"Werklijst">,
          zoekopdracht: { filters: {} },
        },
      },
      { provide: MatDialogRef, useValue: dialogRefMock },
    ],
  });
  const gebruikersvoorkeurenService = TestBed.inject(
    GebruikersvoorkeurenService,
  );
  jest
    .spyOn(gebruikersvoorkeurenService, "createOrUpdateZoekOpdrachten")
    .mockReturnValue(of(undefined) as never);
  const fixture: ComponentFixture<ZoekopdrachtSaveDialogComponent> =
    TestBed.createComponent(ZoekopdrachtSaveDialogComponent);
  fixture.detectChanges();
  return {
    fixture,
    component: fixture.componentInstance,
    gebruikersvoorkeurenService,
    dialogRefMock,
  };
};

describe(ZoekopdrachtSaveDialogComponent.name, () => {
  describe("close()", () => {
    it("closes the dialog", () => {
      const { component, dialogRefMock } = setup();
      component["close"]();
      expect(dialogRefMock.close).toHaveBeenCalled();
    });
  });

  describe("opslaan button", () => {
    it("is disabled when formControl has no value", () => {
      const { fixture } = setup();
      const button: HTMLButtonElement =
        fixture.nativeElement.querySelector("#opslaan_button");
      expect(button.disabled).toBe(true);
    });
  });

  describe("isNew()", () => {
    it("returns true when name is not in existing zoekopdrachten", () => {
      const { component } = setup([makeZoekopdracht("bestaande zoekopdracht")]);
      component["formControl"].setValue("nieuwe naam");
      expect(component["isNew"]()).toBe(true);
    });

    it("returns false when name matches an existing zoekopdracht", () => {
      const { component } = setup([makeZoekopdracht("bestaande zoekopdracht")]);
      component["formControl"].setValue("bestaande zoekopdracht");
      expect(component["isNew"]()).toBe(false);
    });
  });

  describe("opslaan() — new zoekopdracht", () => {
    it("calls createOrUpdateZoekOpdrachten with a new entry", () => {
      const { component, gebruikersvoorkeurenService } = setup();
      component["formControl"].setValue("nieuwe naam");
      component["opslaan"]();
      expect(
        gebruikersvoorkeurenService.createOrUpdateZoekOpdrachten,
      ).toHaveBeenCalledWith(
        expect.objectContaining({
          naam: "nieuwe naam",
          lijstID: "TAKEN_WERKVOORRAAD",
        }),
      );
    });

    it("closes the dialog with true on success", () => {
      const { component, dialogRefMock } = setup();
      component["formControl"].setValue("nieuwe naam");
      component["opslaan"]();
      expect(dialogRefMock.close).toHaveBeenCalledWith(true);
    });
  });

  describe("opslaan() — existing zoekopdracht", () => {
    it("calls createOrUpdateZoekOpdrachten with the existing entry updated", () => {
      const existing = makeZoekopdracht("bestaande zoekopdracht");
      const { component, gebruikersvoorkeurenService } = setup([existing]);
      component["formControl"].setValue("bestaande zoekopdracht");
      component["opslaan"]();
      expect(
        gebruikersvoorkeurenService.createOrUpdateZoekOpdrachten,
      ).toHaveBeenCalledWith(
        expect.objectContaining({
          naam: "bestaande zoekopdracht",
          lijstID: "TAKEN_WERKVOORRAAD",
        }),
      );
    });
  });

  describe("filteredOptions", () => {
    it("filters zoekopdrachten by name input", () => {
      const { component } = setup([
        makeZoekopdracht("gemeente filter"),
        makeZoekopdracht("andere opdracht"),
      ]);
      const result = component["_filter"]("gemeente");
      expect(result).toEqual(["gemeente filter"]);
    });

    it("returns all options when input is empty", () => {
      const { component } = setup([
        makeZoekopdracht("gemeente filter"),
        makeZoekopdracht("andere opdracht"),
      ]);
      const result = component["_filter"]("");
      expect(result).toHaveLength(2);
    });
  });
});
