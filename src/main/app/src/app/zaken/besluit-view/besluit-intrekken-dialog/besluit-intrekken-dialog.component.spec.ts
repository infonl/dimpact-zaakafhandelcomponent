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
import { provideMomentDateAdapter } from "@angular/material-moment-adapter";
import { MAT_DIALOG_DATA, MatDialogRef } from "@angular/material/dialog";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { provideRouter } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { provideTanStackQuery } from "@tanstack/angular-query-experimental";
import moment from "moment";
import { EMPTY } from "rxjs";
import { fromPartial } from "src/test-helpers";
import { sleep, testQueryClient } from "../../../../../setupJest";
import { UtilService } from "../../../core/service/util.service";
import { FoutAfhandelingService } from "../../../fout-afhandeling/fout-afhandeling.service";
import { GeneratedType } from "../../../shared/utils/generated-types";
import { BesluitIntrekkenDialogComponent } from "./besluit-intrekken-dialog.component";

const makeBesluit = (fields: Partial<GeneratedType<"RestDecision">> = {}) =>
  fromPartial<GeneratedType<"RestDecision">>({
    uuid: "besluit-uuid-1",
    ingangsdatum: "2026-01-01",
    vervaldatum: "2026-12-31",
    informatieobjecten: [],
    ...fields,
  });

const setup = (besluit = makeBesluit()) => {
  const dialogRefMock = { close: jest.fn(), disableClose: false };
  TestBed.configureTestingModule({
    imports: [
      BesluitIntrekkenDialogComponent,
      NoopAnimationsModule,
      TranslateModule.forRoot(),
    ],
    providers: [
      provideExperimentalZonelessChangeDetection(),
      provideHttpClient(withInterceptorsFromDi()),
      provideHttpClientTesting(),
      provideRouter([]),
      provideTanStackQuery(testQueryClient),
      provideMomentDateAdapter(),
      { provide: MAT_DIALOG_DATA, useValue: besluit },
      { provide: MatDialogRef, useValue: dialogRefMock },
    ],
  });

  const fixture: ComponentFixture<BesluitIntrekkenDialogComponent> =
    TestBed.createComponent(BesluitIntrekkenDialogComponent);
  fixture.detectChanges();

  const foutAfhandelingService = TestBed.inject(FoutAfhandelingService);
  jest.spyOn(foutAfhandelingService, "foutAfhandelen").mockReturnValue(EMPTY);
  const utilService = TestBed.inject(UtilService);
  jest.spyOn(utilService, "openSnackbar").mockImplementation();

  return {
    fixture,
    component: fixture.componentInstance,
    httpTestingController: TestBed.inject(HttpTestingController),
    dialogRefMock,
    foutAfhandelingService,
    utilService,
  };
};

const fillValidForm = (component: BesluitIntrekkenDialogComponent) => {
  const form = component["form"];
  form.controls.vervalreden.setValue(component["vervalRedenen"][0]);
  form.controls.toelichting.setValue("Reden van intrekken");
};

describe(BesluitIntrekkenDialogComponent.name, () => {
  afterEach(() => {
    testQueryClient.clear();
    jest.clearAllMocks();
  });

  describe("prefill", () => {
    it("prefills the vervaldatum with the current besluit value", () => {
      const { component } = setup();

      expect(
        component["form"].controls.vervaldatum.value?.isSame(
          "2026-12-31",
          "day",
        ),
      ).toBe(true);
    });

    it("offers all vervalredenen except tijdelijk", () => {
      const { component } = setup();

      expect(component["vervalRedenen"]).toEqual([
        {
          label: "besluit.vervalreden.ingetrokken_overheid",
          value: "INGETROKKEN_OVERHEID",
        },
        {
          label: "besluit.vervalreden.ingetrokken_belanghebbende",
          value: "INGETROKKEN_BELANGHEBBENDE",
        },
      ]);
    });
  });

  describe("documenten verstuurd warning", () => {
    it("is hidden when no document has been sent", () => {
      const { component, fixture } = setup();

      expect(component["documentenVerstuurd"]).toBe(false);
      expect(fixture.nativeElement.querySelector(".warning")).toBeNull();
    });

    it("is shown when a linked document has a verzenddatum", () => {
      const { component, fixture } = setup(
        makeBesluit({
          informatieobjecten: [
            fromPartial<GeneratedType<"RestEnkelvoudigInformatieobject">>({
              verzenddatum: "2026-02-01",
            }),
          ],
        }),
      );

      expect(component["documentenVerstuurd"]).toBe(true);
      expect(fixture.nativeElement.querySelector(".warning")).not.toBeNull();
    });
  });

  describe("validation", () => {
    it("is invalid without a toelichting", () => {
      const { component } = setup();
      component["form"].controls.vervalreden.setValue(
        component["vervalRedenen"][0],
      );

      expect(component["form"].invalid).toBe(true);
    });

    it("is invalid without a vervalreden", () => {
      const { component } = setup();
      component["form"].controls.toelichting.setValue("Reden");

      expect(component["form"].invalid).toBe(true);
    });

    it("is invalid when the vervaldatum is before the ingangsdatum", () => {
      const { component } = setup();

      component["form"].controls.vervaldatum.setValue(moment("2025-01-01"));

      expect(component["form"].controls.vervaldatum.invalid).toBe(true);
    });
  });

  describe("intrekken()", () => {
    it("withdraws the besluit with the form values", async () => {
      const { component, httpTestingController } = setup();
      fillValidForm(component);

      component["intrekken"]();
      await sleep();

      const request = httpTestingController.expectOne(
        "/rest/zaken/besluit/intrekken",
      );
      expect(request.request.method).toBe("PUT");
      expect(request.request.body).toEqual(
        expect.objectContaining({
          besluitUuid: "besluit-uuid-1",
          reden: "Reden van intrekken",
          vervalreden: "INGETROKKEN_OVERHEID",
        }),
      );
      request.flush(null);
    });

    it("shows a snackbar and closes with true on success", async () => {
      const { component, httpTestingController, dialogRefMock, utilService } =
        setup();
      fillValidForm(component);

      component["intrekken"]();
      await sleep();

      httpTestingController
        .expectOne("/rest/zaken/besluit/intrekken")
        .flush(null);
      await sleep(100);

      expect(utilService.openSnackbar).toHaveBeenCalledWith(
        "msg.besluit.ingetrokken",
      );
      expect(dialogRefMock.close).toHaveBeenCalledWith(true);
    });

    it("shows an error and keeps the dialog open when withdrawing fails", async () => {
      const {
        component,
        httpTestingController,
        dialogRefMock,
        foutAfhandelingService,
      } = setup();
      fillValidForm(component);

      component["intrekken"]();
      await sleep();

      httpTestingController
        .expectOne("/rest/zaken/besluit/intrekken")
        .flush(null, { status: 500, statusText: "Internal Server Error" });
      await sleep(100);

      expect(foutAfhandelingService.foutAfhandelen).toHaveBeenCalled();
      expect(dialogRefMock.close).not.toHaveBeenCalledWith(true);
    });

    it("disables closing the dialog while the mutation runs", async () => {
      const { component, httpTestingController, dialogRefMock } = setup();
      fillValidForm(component);

      component["intrekken"]();
      await sleep();

      expect(dialogRefMock.disableClose).toBe(true);
      httpTestingController
        .expectOne("/rest/zaken/besluit/intrekken")
        .flush(null);
    });
  });

  describe("close()", () => {
    it("closes the dialog with false", () => {
      const { component, dialogRefMock } = setup();

      component["close"]();

      expect(dialogRefMock.close).toHaveBeenCalledWith(false);
    });
  });
});
