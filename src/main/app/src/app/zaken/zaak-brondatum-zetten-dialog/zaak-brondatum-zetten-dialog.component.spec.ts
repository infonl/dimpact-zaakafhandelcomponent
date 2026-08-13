/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HarnessLoader } from "@angular/cdk/testing";
import { TestbedHarnessEnvironment } from "@angular/cdk/testing/testbed";
import { provideHttpClient } from "@angular/common/http";
import {
  HttpTestingController,
  provideHttpClientTesting,
} from "@angular/common/http/testing";
import { provideZonelessChangeDetection } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatButtonHarness } from "@angular/material/button/testing";
import { MAT_DIALOG_DATA, MatDialogRef } from "@angular/material/dialog";
import { MatInputHarness } from "@angular/material/input/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import moment from "moment";
import { fromPartial } from "src/test-helpers";
import { testQueryClient } from "../../../../setupJest";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZaakBrondatumZettenDialogComponent } from "./zaak-brondatum-zetten-dialog.component";

describe(ZaakBrondatumZettenDialogComponent.name, () => {
  let fixture: ComponentFixture<ZaakBrondatumZettenDialogComponent>;
  let loader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  const mockDialogRef = {
    close: jest.fn(),
    disableClose: false,
  };

  const mockZaak = fromPartial<GeneratedType<"RestZaak">>({
    uuid: "test-zaak-uuid",
    zaaktype: fromPartial<GeneratedType<"RestZaaktype">>({
      uuid: "test-zaaktype-uuid",
      omschrijving: "Test Zaaktype",
    }),
    resultaat: null,
  });

  const mockPlanItem = fromPartial<GeneratedType<"RESTPlanItem">>({
    id: "test-plan-item-id",
    userEventListenerActie: "BRONDATUM_ZETTEN",
  });

  const createTestBed = async (
    zaakMock: GeneratedType<"RestZaak">,
    planItemMock?: GeneratedType<"RESTPlanItem"> | null,
  ) => {
    TestBed.resetTestingModule();

    await TestBed.configureTestingModule({
      imports: [
        ZaakBrondatumZettenDialogComponent,
        TranslateModule.forRoot(),
        NoopAnimationsModule,
      ],
      providers: [
        provideZonelessChangeDetection(),
        provideHttpClient(),
        provideHttpClientTesting(),
        provideQueryClient(testQueryClient),
        { provide: MatDialogRef, useValue: mockDialogRef },
        {
          provide: MAT_DIALOG_DATA,
          useValue: { zaak: zaakMock, planItem: planItemMock },
        },
      ],
    }).compileComponents();

    httpTestingController = TestBed.inject(HttpTestingController);

    fixture = TestBed.createComponent(ZaakBrondatumZettenDialogComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
    fixture.detectChanges();
  };

  beforeEach(async () => {
    mockDialogRef.close = jest.fn();
    await createTestBed(mockZaak, mockPlanItem);
  });

  afterEach(() => {
    testQueryClient.clear();
    httpTestingController.verify();
  });

  describe("form validation", () => {
    it("should disable submit button when brondatum is empty", async () => {
      const submitButton = await loader.getHarness(
        MatButtonHarness.with({ text: /actie\.zaak\.brondatumZetten/ }),
      );
      expect(await submitButton.isDisabled()).toBe(true);
    });

    it("should enable submit button when brondatum is today or later", async () => {
      const input = await loader.getHarness(MatInputHarness);
      await input.setValue(moment().format("YYYY-MM-DD"));
      fixture.detectChanges();

      const submitButton = await loader.getHarness(
        MatButtonHarness.with({ text: /actie\.zaak\.brondatumZetten/ }),
      );
      expect(await submitButton.isDisabled()).toBe(false);
    });

    it("should not allow the form to be submitted when brondatum is before today", async () => {
      const input = await loader.getHarness(MatInputHarness);
      await input.setValue(moment().subtract(1, "day").format("YYYY-MM-DD"));
      fixture.detectChanges();

      const submitButton = await loader.getHarness(
        MatButtonHarness.with({ text: /actie\.zaak\.brondatumZetten/ }),
      );
      expect(await submitButton.isDisabled()).toBe(true);
    });
  });

  describe("actions", () => {
    it("should close dialog when close button is clicked", async () => {
      const closeButton = await loader.getHarness(
        MatButtonHarness.with({ text: /actie\.annuleren/ }),
      );
      await closeButton.click();

      expect(mockDialogRef.close).toHaveBeenCalled();
    });
  });

  describe("Open dialog with a planItem", () => {
    it("should call the planItem mutation with the BRONDATUM_ZETTEN actie on submit", async () => {
      const brondatum = moment().add(1, "day");
      const input = await loader.getHarness(MatInputHarness);
      await input.setValue(brondatum.format("YYYY-MM-DD"));

      const submitButton = await loader.getHarness(
        MatButtonHarness.with({ text: /actie\.zaak\.brondatumZetten/ }),
      );
      await submitButton.click();
      await new Promise(requestAnimationFrame);

      const req = httpTestingController.expectOne(
        `/rest/planitems/doUserEventListenerPlanItem`,
      );
      expect(req.request.method).toEqual("POST");
      expect(req.request.body).toEqual(
        expect.objectContaining({
          actie: "BRONDATUM_ZETTEN",
          planItemInstanceId: "test-plan-item-id",
          zaakUuid: "test-zaak-uuid",
          brondatum: brondatum.startOf("day").toISOString(),
        }),
      );
      req.flush({});
    });
  });

  describe("Open dialog with planItem null", () => {
    beforeEach(async () => {
      const mockZaakWithNoPlanItem = fromPartial<GeneratedType<"RestZaak">>({
        ...mockZaak,
        uuid: "test-zaak-uuid-no-planitem",
      });

      await createTestBed(mockZaakWithNoPlanItem, null);
    });

    it("should call the brondatum mutation on submit", async () => {
      const brondatum = moment().add(1, "day");
      const input = await loader.getHarness(MatInputHarness);
      await input.setValue(brondatum.format("YYYY-MM-DD"));

      const submitButton = await loader.getHarness(
        MatButtonHarness.with({ text: /actie\.zaak\.brondatumZetten/ }),
      );
      await submitButton.click();
      await new Promise(requestAnimationFrame);

      const req = httpTestingController.expectOne(
        `/rest/zaken/zaak/test-zaak-uuid-no-planitem/brondatum`,
      );
      expect(req.request.method).toEqual("PUT");
      expect(req.request.body).toEqual(
        expect.objectContaining({
          brondatum: brondatum.startOf("day").toISOString(),
        }),
      );
      req.flush({});
    });

    it("should close the dialog with true on a successful submit", async () => {
      const brondatum = moment().add(1, "day");
      const input = await loader.getHarness(MatInputHarness);
      await input.setValue(brondatum.format("YYYY-MM-DD"));

      const submitButton = await loader.getHarness(
        MatButtonHarness.with({ text: /actie\.zaak\.brondatumZetten/ }),
      );
      await submitButton.click();
      await new Promise(requestAnimationFrame);

      httpTestingController
        .expectOne(`/rest/zaken/zaak/test-zaak-uuid-no-planitem/brondatum`)
        .flush({});
      await new Promise(requestAnimationFrame);

      expect(mockDialogRef.close).toHaveBeenCalledWith(true);
    });
  });

  describe("brondatumLabel", () => {
    it("should use the datumKenmerkOmschrijving of the resultaattype when datumKenmerkVerplicht is true", async () => {
      const mockZaakWithResultaat = fromPartial<GeneratedType<"RestZaak">>({
        ...mockZaak,
        uuid: "test-zaak-uuid-with-resultaat",
        resultaat: fromPartial({
          resultaattype: fromPartial<GeneratedType<"RestResultaattype">>({
            datumKenmerkVerplicht: true,
            datumKenmerkOmschrijving: "Fake datumkenmerk omschrijving",
          }),
        }),
      });

      await createTestBed(mockZaakWithResultaat, mockPlanItem);

      expect(fixture.componentInstance["brondatumLabel"]).toBe(
        "Fake datumkenmerk omschrijving",
      );
    });
  });
});
