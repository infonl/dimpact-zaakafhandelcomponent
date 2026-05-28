/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  provideHttpClient,
  withInterceptorsFromDi,
} from "@angular/common/http";
import {
  HttpTestingController,
  provideHttpClientTesting,
} from "@angular/common/http/testing";
import { provideExperimentalZonelessChangeDetection } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MAT_DIALOG_DATA, MatDialogRef } from "@angular/material/dialog";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { provideRouter } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { provideTanStackQuery } from "@tanstack/angular-query-experimental";
import { sleep, testQueryClient } from "../../../../setupJest";
import { TaakZoekObject } from "../../zoeken/model/taken/taak-zoek-object";
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
      provideExperimentalZonelessChangeDetection(),
      provideHttpClient(withInterceptorsFromDi()),
      provideHttpClientTesting(),
      provideRouter([]),
      provideTanStackQuery(testQueryClient),
      {
        provide: MAT_DIALOG_DATA,
        useValue: { taken, screenEventResourceId: "screen-resource-id" },
      },
      { provide: MatDialogRef, useValue: dialogRefMock },
    ],
  });
  const fixture: ComponentFixture<TakenVrijgevenDialogComponent> =
    TestBed.createComponent(TakenVrijgevenDialogComponent);
  fixture.detectChanges();
  return {
    fixture,
    component: fixture.componentInstance,
    httpTestingController: TestBed.inject(HttpTestingController),
    dialogRefMock,
  };
};

describe(TakenVrijgevenDialogComponent.name, () => {
  afterEach(() => {
    testQueryClient.clear();
    jest.clearAllMocks();
  });

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

  describe("vrijgeven()", () => {
    it("passes reden and filtered taken to the API", async () => {
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
      const { component, httpTestingController } = setup(taken);

      component["form"].controls.reden.setValue("testopmerking");
      component["vrijgeven"]();
      await new Promise(requestAnimationFrame);

      const req = httpTestingController.expectOne(
        "/rest/taken/lijst/vrijgeven",
      );
      expect(req.request.method).toBe("PUT");
      expect(req.request.body).toEqual(
        expect.objectContaining({
          reden: "testopmerking",
          screenEventResourceId: "screen-resource-id",
          taken: [{ taakId: "t1", zaakUuid: "uuid-1" }],
        }),
      );
      req.flush(null);
    });

    it("closes the dialog with true on success", async () => {
      const { component, httpTestingController, dialogRefMock } = setup();

      component["form"].controls.reden.setValue("reden");
      component["vrijgeven"]();
      await new Promise(requestAnimationFrame);

      httpTestingController
        .expectOne("/rest/taken/lijst/vrijgeven")
        .flush(null);
      await sleep();

      expect(dialogRefMock.close).toHaveBeenCalledWith(true);
    });

    it("sets disableClose on the dialog when mutation starts", async () => {
      const { component, httpTestingController, dialogRefMock } = setup();

      component["form"].controls.reden.setValue("reden");
      component["vrijgeven"]();
      await new Promise(requestAnimationFrame);

      expect(dialogRefMock.disableClose).toBe(true);
      httpTestingController
        .expectOne("/rest/taken/lijst/vrijgeven")
        .flush(null);
    });
  });
});
