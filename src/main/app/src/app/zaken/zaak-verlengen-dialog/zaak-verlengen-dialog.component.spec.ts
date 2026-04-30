/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HarnessLoader } from "@angular/cdk/testing";
import { TestbedHarnessEnvironment } from "@angular/cdk/testing/testbed";
import { provideHttpClient } from "@angular/common/http";
import {
  HttpTestingController,
  provideHttpClientTesting,
} from "@angular/common/http/testing";
import { provideExperimentalZonelessChangeDetection } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatButtonHarness } from "@angular/material/button/testing";
import { provideNativeDateAdapter } from "@angular/material/core";
import { MAT_DIALOG_DATA, MatDialogRef } from "@angular/material/dialog";
import { MatErrorHarness } from "@angular/material/form-field/testing";
import { MatInputHarness } from "@angular/material/input/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { notifyManager } from "@tanstack/query-core";
import { fromPartial } from "src/test-helpers";
import { testQueryClient } from "../../../../setupJest";
import { GeneratedType } from "../../shared/utils/generated-types";
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
    uiterlijkeEinddatumAfdoening: new Date().toISOString(),
  });

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        ZaakVerlengenDialogComponent,
        NoopAnimationsModule,
        TranslateModule.forRoot(),
      ],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideNativeDateAdapter(),
        provideExperimentalZonelessChangeDetection(),
        provideQueryClient(testQueryClient),
        {
          provide: MatDialogRef,
          useValue: { close: jest.fn(), disableClose: false },
        },
        {
          provide: MAT_DIALOG_DATA,
          useValue: {
            zaak: { ...mockZaak },
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
  });

  describe("Buttons and form submission", () => {
    it("makes HTTP PATCH request with form payload when verlengen is called", async () => {
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
      const dialogRefSpy = jest.spyOn(dialogRef, "close");

      const cancelButton = await loader.getHarness(
        MatButtonHarness.with({ text: /annuleren/i }),
      );
      await cancelButton.click();
      await new Promise(requestAnimationFrame);

      expect(dialogRefSpy).toHaveBeenCalled();
    });
  });

  describe("Check errors", () => {
    it("should show an error when verlengingsDuur exceeds the zaaktype limit", async () => {
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
