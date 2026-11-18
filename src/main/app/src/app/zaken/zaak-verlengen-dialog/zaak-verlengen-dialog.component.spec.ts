/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HarnessLoader } from "@angular/cdk/testing";
import { TestbedHarnessEnvironment } from "@angular/cdk/testing/testbed";
import { provideHttpClient, withXhr } from "@angular/common/http";
import {
  HttpTestingController,
  provideHttpClientTesting,
} from "@angular/common/http/testing";
import { provideZonelessChangeDetection } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatButtonHarness } from "@angular/material/button/testing";
import { MAT_DIALOG_DATA, MatDialogRef } from "@angular/material/dialog";
import { MatErrorHarness } from "@angular/material/form-field/testing";
import { MatInputHarness } from "@angular/material/input/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { provideTranslateService } from "@ngx-translate/core";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { notifyManager } from "@tanstack/query-core";
import { screen } from "@testing-library/angular";
import { userEvent } from "@testing-library/user-event";
import { fromPartial } from "src/test-helpers";
import { testQueryClient } from "../../../../setupJest";
import { provideZacDateAdapter } from "../../date-time";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZakenService } from "../zaken.service";
import { ZaakVerlengenDialogComponent } from "./zaak-verlengen-dialog.component";

describe("ZaakVerlengenDialogComponent", () => {
  beforeEach(() => notifyManager.setScheduler((fn) => fn()));
  afterEach(() => notifyManager.setScheduler((fn) => setTimeout(fn, 0)));

  let component: ZaakVerlengenDialogComponent;
  let fixture: ComponentFixture<ZaakVerlengenDialogComponent>;
  let dialogRef: MatDialogRef<ZaakVerlengenDialogComponent>;
  let loader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  const mockZaak = fromPartial<GeneratedType<"RestZaak">>({
    uuid: "b7e8f9a2-4c3d-11ee-bb2f-0242ac130003",
    zaaktype: { verlengingstermijn: 10 },
    einddatumGepland: null,
    uiterlijkeEinddatumAfdoening: "2026-01-30",
  });

  async function setup(zaak = mockZaak) {
    await TestBed.configureTestingModule({
      imports: [ZaakVerlengenDialogComponent, NoopAnimationsModule],
      providers: [
        provideTranslateService(),
        provideHttpClient(withXhr()),
        provideHttpClientTesting(),
        provideZacDateAdapter(),
        provideZonelessChangeDetection(),
        provideQueryClient(testQueryClient),
        {
          provide: MatDialogRef,
          useValue: { close: jest.fn(), disableClose: false },
        },
        {
          provide: MAT_DIALOG_DATA,
          useValue: {
            zaak: { ...zaak },
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ZaakVerlengenDialogComponent);
    dialogRef = TestBed.inject(MatDialogRef);
    loader = TestbedHarnessEnvironment.loader(fixture);
    httpTestingController = TestBed.inject(HttpTestingController);

    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  async function verlengen(verlengduur: string) {
    const user = userEvent.setup();
    await user.type(screen.getByLabelText(/verlengduur/i), verlengduur);
    await user.type(screen.getByLabelText(/reden/i), "Reden verlenging");
    fixture.detectChanges();
    await user.click(
      screen.getByRole("button", { name: /actie.zaak.verlengen/ }),
    );
    await new Promise(requestAnimationFrame);

    return httpTestingController.expectOne(
      `/rest/zaken/zaak/${mockZaak.uuid}/verlenging`,
    );
  }

  describe("Buttons and form submission", () => {
    it("sends only the verlengduur, so the backend moves the dates of the zaak", async () => {
      await setup(
        fromPartial<GeneratedType<"RestZaak">>({
          ...mockZaak,
          einddatumGepland: "2026-01-20",
        }),
      );

      const request = await verlengen("5");

      expect(request.request.body).toEqual({
        duurDagen: 5,
        redenVerlenging: "Reden verlenging",
        shouldTakenVerlengen: false,
      });
    });

    it("makes HTTP PATCH request with form payload when verlengen is called", async () => {
      await setup();
      component["form"].patchValue(
        {
          duurDagen: 5,
          redenVerlenging: "Reden verlenging",
        },
        { emitEvent: false },
      );

      component["verlengen"]();
      await new Promise(requestAnimationFrame);

      const req = httpTestingController.expectOne(
        (request) =>
          request.method === "PATCH" &&
          request.url.includes(`/rest/zaken/zaak/${mockZaak.uuid}/verlenging`),
      );
      expect(req.request.method).toEqual("PATCH");
      expect(req.request.body).toEqual(
        expect.objectContaining({
          duurDagen: 5,
          redenVerlenging: "Reden verlenging",
        }),
      );
      req.flush({});
    });

    it("should call close() when annuleren button is clicked", async () => {
      await setup();
      const dialogRefSpy = jest.spyOn(dialogRef, "close");

      const cancelButton = await loader.getHarness(
        MatButtonHarness.with({ text: /annuleren/i }),
      );
      await cancelButton.click();
      await new Promise(requestAnimationFrame);

      expect(dialogRefSpy).toHaveBeenCalled();
    });

    it("keeps the submit button disabled after a successful verlenging", async () => {
      await setup();
      component["form"].patchValue(
        { duurDagen: 5, redenVerlenging: "Reden verlenging" },
        { emitEvent: false },
      );

      component["verlengen"]();
      await new Promise(requestAnimationFrame);
      httpTestingController
        .expectOne(
          (request) =>
            request.method === "PATCH" &&
            request.url.includes(
              `/rest/zaken/zaak/${mockZaak.uuid}/verlenging`,
            ),
        )
        .flush({});
      await new Promise(requestAnimationFrame);
      fixture.detectChanges();

      const submitButton = await loader.getHarness(
        MatButtonHarness.with({ selector: 'button[type="submit"]' }),
      );
      expect(await submitButton.isDisabled()).toBe(true);
    });

    it("caches the zaak returned by verlenging and closes the dialog", async () => {
      await setup();
      const zakenService = TestBed.inject(ZakenService);
      const cacheZaakSpy = jest.spyOn(zakenService, "cacheZaak");
      const dialogRefSpy = jest.spyOn(dialogRef, "close");
      component["form"].patchValue(
        { duurDagen: 5, redenVerlenging: "Reden verlenging" },
        { emitEvent: false },
      );

      component["verlengen"]();
      await new Promise(requestAnimationFrame);

      const updatedZaak = fromPartial<GeneratedType<"RestZaak">>({
        uuid: mockZaak.uuid,
        uiterlijkeEinddatumAfdoening: "2026-02-04",
      });
      httpTestingController
        .expectOne(
          (request) =>
            request.method === "PATCH" &&
            request.url.includes(
              `/rest/zaken/zaak/${mockZaak.uuid}/verlenging`,
            ),
        )
        .flush(updatedZaak);
      await new Promise(requestAnimationFrame);

      expect(cacheZaakSpy).toHaveBeenCalledWith(updatedZaak);
      expect(dialogRefSpy).toHaveBeenCalledWith(updatedZaak);
    });
  });

  describe("Check errors", () => {
    it("should show an error when verlengingsDuur exceeds the zaaktype limit", async () => {
      await setup();
      const inputs = await loader.getAllHarnesses(MatInputHarness);
      const [verlengingsDuur, , redenVerlenging] = inputs;

      await verlengingsDuur.setValue("30");
      await redenVerlenging.setValue("Reden verlenging");

      const submitButton = await loader.getHarness(
        MatButtonHarness.with({ selector: 'button[type="submit"]' }),
      );
      await submitButton.click();
      await new Promise(requestAnimationFrame);
      const errorHarness = await loader.getHarness(MatErrorHarness);
      const errorText = await errorHarness.getText();

      expect(errorText).toContain("validators.max");
    });
  });
});
