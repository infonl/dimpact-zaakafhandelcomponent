/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { provideHttpClient } from "@angular/common/http";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MAT_DIALOG_DATA, MatDialogRef } from "@angular/material/dialog";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { provideRouter } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { of } from "rxjs";
import { TaakZoekObject } from "../../zoeken/model/taken/taak-zoek-object";
import { TakenService } from "../taken.service";
import { TakenVrijgevenDialogComponent } from "./taken-vrijgeven-dialog.component";

const makeTaak = (fields: Partial<TaakZoekObject> = {}): TaakZoekObject =>
  ({
    id: "taak-1",
    zaakUuid: "zaak-uuid-1",
    behandelaarGebruikersnaam: "user1",
    ...fields,
  }) as Partial<TaakZoekObject> as unknown as TaakZoekObject;

const setup = (taken: TaakZoekObject[] = [makeTaak()]) => {
  const dialogRefMock = { close: jest.fn(), disableClose: false };
  TestBed.configureTestingModule({
    imports: [
      TakenVrijgevenDialogComponent,
      NoopAnimationsModule,
      TranslateModule.forRoot(),
    ],
    providers: [
      provideHttpClient(),
      provideRouter([]),
      {
        provide: MAT_DIALOG_DATA,
        useValue: { taken, screenEventResourceId: "screen-resource-id" },
      },
      { provide: MatDialogRef, useValue: dialogRefMock },
    ],
  });
  const takenService = TestBed.inject(TakenService);
  jest
    .spyOn(takenService, "vrijgevenVanuitLijst")
    .mockReturnValue(of(undefined) as never);
  const fixture: ComponentFixture<TakenVrijgevenDialogComponent> =
    TestBed.createComponent(TakenVrijgevenDialogComponent);
  fixture.detectChanges();
  return {
    fixture,
    component: fixture.componentInstance,
    takenService,
    dialogRefMock,
  };
};

describe(TakenVrijgevenDialogComponent.name, () => {
  describe("with a single taak", () => {
    it("shows singular message", () => {
      const { fixture } = setup([makeTaak()]);
      const paragraphs: NodeListOf<HTMLElement> =
        fixture.nativeElement.querySelectorAll("p");
      const visible = Array.from(paragraphs).filter(
        (p) => p.offsetParent !== null || p.textContent?.trim(),
      );
      expect(
        visible.some((p) => p.textContent?.includes("msg.vrijgeven.taak")),
      ).toBe(true);
    });
  });

  describe("with multiple taken", () => {
    it("shows plural message", () => {
      const { fixture } = setup([
        makeTaak({ id: "t1" }),
        makeTaak({ id: "t2" }),
      ]);
      const paragraphs: NodeListOf<HTMLElement> =
        fixture.nativeElement.querySelectorAll("p");
      expect(
        Array.from(paragraphs).some((p) =>
          p.textContent?.includes("msg.vrijgeven.taken"),
        ),
      ).toBe(true);
    });
  });

  describe("close()", () => {
    it("closes the dialog with false", () => {
      const { component, dialogRefMock } = setup();
      component["close"]();
      expect(dialogRefMock.close).toHaveBeenCalledWith(false);
    });
  });

  describe("vrijgeven button", () => {
    it("is disabled when there are no taken", () => {
      const { fixture } = setup([]);
      const button: HTMLButtonElement = fixture.nativeElement.querySelector(
        "#taakVrijgeven_button",
      );
      expect(button.disabled).toBe(true);
    });

    it("is disabled when loading", () => {
      const { fixture, component } = setup();
      component["loading"] = true;
      fixture.detectChanges();
      const button: HTMLButtonElement = fixture.nativeElement.querySelector(
        "#taakVrijgeven_button",
      );
      expect(button.disabled).toBe(true);
    });
  });

  describe("annuleren button", () => {
    it("is disabled when loading", () => {
      const { fixture, component } = setup();
      component["loading"] = true;
      fixture.detectChanges();
      const button: HTMLButtonElement = fixture.nativeElement.querySelector(
        "#dialogClose_button",
      );
      expect(button.disabled).toBe(true);
    });

  });

  describe("vrijgeven() — reden", () => {
    it("passes reden from form to service", () => {
      const { component, takenService } = setup([makeTaak()]);
      component["form"].controls.reden.setValue("testopmerking");
      component["vrijgeven"]();
      expect(takenService.vrijgevenVanuitLijst).toHaveBeenCalledWith(
        expect.objectContaining({ reden: "testopmerking" }),
      );
    });
  });

  describe("vrijgeven()", () => {
    it("calls vrijgevenVanuitLijst only with taken that have a behandelaarGebruikersnaam", () => {
      const taken = [
        makeTaak({
          id: "t1",
          zaakUuid: "uuid-1",
          behandelaarGebruikersnaam: "user1",
        }),
        makeTaak({
          id: "t2",
          zaakUuid: "uuid-2",
          behandelaarGebruikersnaam: undefined,
        }),
      ];
      const { component, takenService } = setup(taken);
      component["vrijgeven"]();
      expect(takenService.vrijgevenVanuitLijst).toHaveBeenCalledWith(
        expect.objectContaining({
          taken: [{ taakId: "t1", zaakUuid: "uuid-1" }],
          screenEventResourceId: "screen-resource-id",
        }),
      );
    });

    it("closes the dialog with true on success", () => {
      const { component, dialogRefMock } = setup();
      component["vrijgeven"]();
      expect(dialogRefMock.close).toHaveBeenCalledWith(true);
    });
  });
});
