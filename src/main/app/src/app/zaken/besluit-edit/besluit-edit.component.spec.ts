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
import {
  ComponentRef,
  provideExperimentalZonelessChangeDetection,
} from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { provideMomentDateAdapter } from "@angular/material-moment-adapter";
import { MatButtonHarness } from "@angular/material/button/testing";
import { MatDrawer } from "@angular/material/sidenav";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import moment from "moment";
import { EMPTY, of } from "rxjs";
import { fromPartial } from "src/test-helpers";
import { sleep, testQueryClient } from "../../../../setupJest";
import { UtilService } from "../../core/service/util.service";
import { FoutAfhandelingService } from "../../fout-afhandeling/fout-afhandeling.service";
import { InformatieObjectenService } from "../../informatie-objecten/informatie-objecten.service";
import { GeneratedType } from "../../shared/utils/generated-types";
import { BesluitEditComponent } from "./besluit-edit.component";

describe(BesluitEditComponent.name, () => {
  let component: BesluitEditComponent;
  let componentRef: ComponentRef<BesluitEditComponent>;
  let fixture: ComponentFixture<BesluitEditComponent>;
  let loader: HarnessLoader;
  let httpTestingController: HttpTestingController;
  let informatieObjectenService: InformatieObjectenService;
  let foutAfhandelingService: FoutAfhandelingService;
  let utilService: UtilService;

  const mockSideNav = fromPartial<MatDrawer>({
    close: jest.fn().mockReturnValue(Promise.resolve("close")),
  });

  const mockDocuments = [
    fromPartial<GeneratedType<"RestEnkelvoudigInformatieobject">>({
      uuid: "document-uuid-1",
      titel: "Document 1",
      bestandsnaam: "document-1.pdf",
    }),
    fromPartial<GeneratedType<"RestEnkelvoudigInformatieobject">>({
      uuid: "document-uuid-2",
      titel: "Document 2",
      bestandsnaam: "document-2.pdf",
    }),
  ];

  const makeBesluit = (fields: Partial<GeneratedType<"RestBesluit">> = {}) =>
    fromPartial<GeneratedType<"RestBesluit">>({
      uuid: "besluit-uuid-1",
      besluittype: fromPartial<GeneratedType<"RestBesluitType">>({
        id: "besluittype-id-1",
        naam: "Besluittype 1",
        publication: { enabled: false },
      }),
      ingangsdatum: "2026-01-01",
      vervaldatum: "2026-12-31",
      toelichting: "Bestaande toelichting",
      informatieobjecten: [mockDocuments[0]],
      ...fields,
    });

  const setupComponent = async (besluit = makeBesluit()) => {
    fixture = TestBed.createComponent(BesluitEditComponent);
    component = fixture.componentInstance;
    componentRef = fixture.componentRef;

    componentRef.setInput("sideNav", mockSideNav);
    componentRef.setInput("zaak", fromPartial({ uuid: "zaak-uuid-1" }));
    componentRef.setInput("besluit", besluit);

    testQueryClient.setQueryData(
      ["besluit-documenten", "zaak-uuid-1", besluit.besluittype?.id],
      mockDocuments,
    );

    loader = TestbedHarnessEnvironment.loader(fixture);
    fixture.detectChanges();
    await fixture.whenStable();
    await sleep();
    fixture.detectChanges();
    await fixture.whenStable();
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        BesluitEditComponent,
        TranslateModule.forRoot(),
        NoopAnimationsModule,
      ],
      providers: [
        provideExperimentalZonelessChangeDetection(),
        provideHttpClient(),
        provideHttpClientTesting(),
        provideMomentDateAdapter(),
        provideQueryClient(testQueryClient),
        InformatieObjectenService,
        UtilService,
      ],
    }).compileComponents();

    informatieObjectenService = TestBed.inject(InformatieObjectenService);
    foutAfhandelingService = TestBed.inject(FoutAfhandelingService);
    utilService = TestBed.inject(UtilService);
    httpTestingController = TestBed.inject(HttpTestingController);

    jest
      .spyOn(informatieObjectenService, "listEnkelvoudigInformatieobjecten")
      .mockReturnValue(of(mockDocuments));
    jest.spyOn(utilService, "openSnackbar");
    jest.spyOn(foutAfhandelingService, "foutAfhandelen").mockReturnValue(EMPTY);
  });

  afterEach(() => {
    testQueryClient.clear();
    httpTestingController.verify();
    jest.clearAllMocks();
  });

  describe("prefill", () => {
    it("fills the form with the current besluit values", async () => {
      await setupComponent();

      const { controls } = component["form"];
      expect(controls.besluittype.value).toBe("Besluittype 1");
      expect(controls.ingangsdatum.value?.isSame("2026-01-01", "day")).toBe(
        true,
      );
      expect(controls.vervaldatum.value?.isSame("2026-12-31", "day")).toBe(
        true,
      );
      expect(controls.toelichting.value).toBe("Bestaande toelichting");
    });

    it("renders the besluittype field as read-only", async () => {
      await setupComponent();

      expect(component["form"].controls.besluittype.disabled).toBe(true);
    });
  });

  describe("documents", () => {
    it("loads the documents linked to the besluittype for the active zaak", async () => {
      await setupComponent();

      expect(
        informatieObjectenService.listEnkelvoudigInformatieobjecten,
      ).toHaveBeenCalledWith({
        zaakUUID: "zaak-uuid-1",
        besluittypeUUID: "besluittype-id-1",
      });
    });

    it("pre-selects the documents already linked to the besluit", async () => {
      await setupComponent();

      expect(component["form"].controls.documenten.value).toEqual([
        mockDocuments[0],
      ]);
    });
  });

  describe("validation", () => {
    it("is invalid without an ingangsdatum", async () => {
      await setupComponent();
      component["form"].controls.reden.setValue("reden");

      component["form"].controls.ingangsdatum.setValue(null);

      expect(component["form"].invalid).toBe(true);
    });

    it("is invalid when the vervaldatum is before the ingangsdatum", async () => {
      await setupComponent();
      component["form"].controls.ingangsdatum.setValue(moment("2026-06-10"));

      component["form"].controls.vervaldatum.setValue(moment("2026-06-05"));

      expect(component["form"].controls.vervaldatum.invalid).toBe(true);
    });

    it("shows a readable message when the vervaldatum is before the ingangsdatum", async () => {
      await setupComponent();
      component["form"].controls.ingangsdatum.setValue(moment("2026-06-10"));

      component["form"].controls.vervaldatum.setValue(moment("2026-06-05"));

      expect(
        component["form"].controls.vervaldatum.errors?.custom?.message,
      ).toBe("msg.error.date.invalid.datum.vervaldatum-voor-ingangsdatum");
    });

    it("marks the prefilled vervaldatum touched and shows the readable message when the ingangsdatum is moved past it", async () => {
      await setupComponent();
      expect(component["form"].controls.vervaldatum.touched).toBe(false);

      component["form"].controls.ingangsdatum.setValue(moment("2027-06-10"));

      expect(component["form"].controls.vervaldatum.touched).toBe(true);
      expect(
        component["form"].controls.vervaldatum.errors?.custom?.message,
      ).toBe("msg.error.date.invalid.datum.vervaldatum-voor-ingangsdatum");
    });

    it("is invalid without a reden", async () => {
      await setupComponent();

      expect(component["form"].controls.reden.invalid).toBe(true);
    });

    it("accepts a toelichting of up to 1000 characters", async () => {
      await setupComponent();

      component["form"].controls.toelichting.setValue("a".repeat(1000));

      expect(component["form"].controls.toelichting.valid).toBe(true);
    });

    it("is invalid when the toelichting exceeds 1000 characters", async () => {
      await setupComponent();

      component["form"].controls.toelichting.setValue("a".repeat(1001));

      expect(
        component["form"].controls.toelichting.errors?.maxlength,
      ).toBeTruthy();
    });

    it("accepts a reden of up to 80 characters", async () => {
      await setupComponent();

      component["form"].controls.reden.setValue("a".repeat(80));

      expect(component["form"].controls.reden.valid).toBe(true);
    });

    it("is invalid when the reden exceeds 80 characters", async () => {
      await setupComponent();

      component["form"].controls.reden.setValue("a".repeat(81));

      expect(component["form"].controls.reden.errors?.maxlength).toBeTruthy();
    });
  });

  const countDateFields = () =>
    fixture.nativeElement.querySelectorAll("zac-date").length;

  describe("publication section", () => {
    it("is hidden when the besluittype does not require publication", async () => {
      await setupComponent();

      // Only ingangsdatum and vervaldatum are rendered.
      expect(countDateFields()).toBe(2);
    });

    it("is shown when the besluittype requires publication", async () => {
      await setupComponent(
        makeBesluit({
          besluittype: fromPartial<GeneratedType<"RestBesluitType">>({
            id: "besluittype-id-2",
            naam: "Besluittype 2",
            publication: { enabled: true, responseTermDays: 6 },
          }),
        }),
      );

      // ingangsdatum, vervaldatum + publicationDate, lastResponseDate.
      expect(countDateFields()).toBe(4);
    });
  });

  describe("publication dates", () => {
    const makePublicationBesluit = (
      fields: Partial<GeneratedType<"RestBesluit">> = {},
    ) =>
      makeBesluit({
        besluittype: fromPartial<GeneratedType<"RestBesluitType">>({
          id: "besluittype-id-2",
          naam: "Besluittype 2",
          publication: { enabled: true, responseTermDays: 6 },
        }),
        ...fields,
      });

    it("auto-fills lastResponseDate from publicationDate + responseTermDays", async () => {
      await setupComponent(makePublicationBesluit());

      component["form"].controls.publicationDate.setValue(moment("2026-03-01"));

      expect(
        component["form"].controls.lastResponseDate.value?.isSame(
          moment("2026-03-07"),
          "day",
        ),
      ).toBe(true);
    });

    it("applies the lastResponseDate minimum on load when publication is required", async () => {
      await setupComponent(
        makePublicationBesluit({ vervaldatum: "2026-12-31" }),
      );

      expect(component["lastResponseDateMinValidator"]).not.toBeNull();
    });

    it("shows a readable message when the lastResponseDate is before its minimum", async () => {
      await setupComponent(makePublicationBesluit());
      component["form"].controls.publicationDate.setValue(moment("2026-03-01"));

      component["form"].controls.lastResponseDate.setValue(
        moment("2026-03-01"),
      );

      expect(
        component["form"].controls.lastResponseDate.errors?.custom?.message,
      ).toBe("msg.error.date.invalid.datum.reactiedatum-voor-publicatiedatum");
    });

    it("resets the lastResponseDate and its minimum when the publicationDate is cleared", async () => {
      await setupComponent(makePublicationBesluit());
      component["form"].controls.publicationDate.setValue(moment("2026-03-01"));
      expect(component["form"].controls.lastResponseDate.value).not.toBeNull();

      component["form"].controls.publicationDate.setValue(null);

      expect(component["form"].controls.lastResponseDate.value).toBeNull();
      expect(component["lastResponseDateMinValidator"]).toBeNull();
    });
  });

  describe("submit", () => {
    it("updates the besluit and emits besluitGewijzigd on success", async () => {
      await setupComponent();
      const emitSpy = jest.spyOn(component["besluitGewijzigd"], "emit");
      component["form"].controls.reden.setValue("Wijziging reden");

      component["submit"]();
      await sleep();

      const request = httpTestingController.expectOne({
        method: "PUT",
        url: "/rest/zaken/besluit",
      });
      expect(request.request.body).toEqual(
        expect.objectContaining({
          besluitUuid: "besluit-uuid-1",
          reden: "Wijziging reden",
          informatieobjecten: ["document-uuid-1"],
          ingangsdatum: moment("2026-01-01").toISOString(),
          vervaldatum: moment("2026-12-31").toISOString(),
        }),
      );
      request.flush(null);
      await sleep(100);

      expect(utilService.openSnackbar).toHaveBeenCalledWith(
        "msg.besluit.gewijzigd",
      );
      expect(emitSpy).toHaveBeenCalledWith(true);
    });

    it("includes the publication dates in the payload when publication is required", async () => {
      await setupComponent(
        makeBesluit({
          besluittype: fromPartial<GeneratedType<"RestBesluitType">>({
            id: "besluittype-id-2",
            naam: "Besluittype 2",
            publication: { enabled: true, responseTermDays: 6 },
          }),
          publicationDate: "2026-02-01",
          lastResponseDate: "2026-02-07",
        }),
      );
      component["form"].controls.reden.setValue("Wijziging reden");

      component["submit"]();
      await sleep();

      const request = httpTestingController.expectOne({
        method: "PUT",
        url: "/rest/zaken/besluit",
      });
      expect(request.request.body).toEqual(
        expect.objectContaining({
          publicationDate: moment("2026-02-01").toISOString(),
          lastResponseDate: moment("2026-02-07").toISOString(),
        }),
      );
      request.flush(null);
      await sleep(100);
    });

    it("shows an error and keeps the panel open when updating fails", async () => {
      await setupComponent();
      const emitSpy = jest.spyOn(component["besluitGewijzigd"], "emit");
      component["form"].controls.reden.setValue("Wijziging reden");

      component["submit"]();
      await sleep();

      httpTestingController
        .expectOne({ method: "PUT", url: "/rest/zaken/besluit" })
        .flush(null, { status: 500, statusText: "Internal Server Error" });
      await sleep(100);

      expect(foutAfhandelingService.foutAfhandelen).toHaveBeenCalled();
      expect(emitSpy).not.toHaveBeenCalled();
      expect(mockSideNav.close).not.toHaveBeenCalled();
    });
  });

  describe("buttons", () => {
    it("closes the sideNav when the cancel button is clicked", async () => {
      await setupComponent();

      const cancelButton = await loader.getHarness(
        MatButtonHarness.with({ text: /actie\.annuleren/ }),
      );
      await cancelButton.click();

      expect(mockSideNav.close).toHaveBeenCalled();
    });
  });
});
