/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HarnessLoader } from "@angular/cdk/testing";
import { TestbedHarnessEnvironment } from "@angular/cdk/testing/testbed";
import { provideHttpClient } from "@angular/common/http";
import { provideHttpClientTesting } from "@angular/common/http/testing";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatIconHarness } from "@angular/material/icon/testing";
import { MatTabGroupHarness } from "@angular/material/tabs/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { provideRouter } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { within } from "@testing-library/angular";
import { fromPartial } from "src/test-helpers";
import { testQueryClient } from "../../../../../setupJest";
import { GeneratedType } from "../../../shared/utils/generated-types";
import { ZaakDetailsCardComponent } from "./zaak-details-card.component";

describe(ZaakDetailsCardComponent.name, () => {
  let fixture: ComponentFixture<ZaakDetailsCardComponent>;
  let loader: HarnessLoader;

  const zaak = fromPartial<GeneratedType<"RestZaak">>({
    uuid: "1234",
    identificatie: "ZAAK-2026-0001",
    zaaktype: fromPartial<GeneratedType<"RestZaaktype">>({
      omschrijving: "fakeZaaktypeOmschrijving",
    }),
    indicaties: [],
    rechten: { behandelen: true },
    groep: {},
    vertrouwelijkheidaanduiding: "OPENBAAR",
    gerelateerdeZaken: [],
  });

  const screen = () => within(fixture.nativeElement as HTMLElement);

  const renderCard = (
    inputs: Partial<{
      zaak: GeneratedType<"RestZaak">;
      bagObjecten: GeneratedType<"RESTBAGObjectGegevens">[];
      showBetrokkeneKoppelingen: boolean;
    }> = {},
  ) => {
    fixture.componentRef.setInput("zaak", inputs.zaak ?? zaak);
    fixture.componentRef.setInput("bagObjecten", inputs.bagObjecten ?? []);
    fixture.componentRef.setInput(
      "showBetrokkeneKoppelingen",
      inputs.showBetrokkeneKoppelingen ?? false,
    );
    fixture.detectChanges();
  };

  // each tab label renders its mat-icon ligature before the translation key
  const tabLabels = async () => {
    const tabGroup = await loader.getHarness(MatTabGroupHarness);
    const tabs = await tabGroup.getTabs();
    const labels = await Promise.all(tabs.map((tab) => tab.getLabel()));
    return labels.map((label) => label.split(/\s+/).pop());
  };

  const locatieEditButton = () =>
    screen().queryByRole("button", { name: "actie.zaak.locatie.wijzigen" });

  const zaakEditButton = () =>
    screen().queryByRole("button", { name: "actie.zaak.wijzigen" });

  const openTab = async (label: RegExp) => {
    const tabGroup = await loader.getHarness(MatTabGroupHarness);
    await tabGroup.selectTab({ label });
    fixture.detectChanges();
  };

  let originalResizeObserver: typeof global.ResizeObserver;

  beforeEach(() => {
    // the locatie tab renders an OpenLayers map, which observes its container
    originalResizeObserver = global.ResizeObserver;
    global.ResizeObserver = class {
      observe() {}
      unobserve() {}
      disconnect() {}
    };
  });

  afterEach(() => {
    global.ResizeObserver = originalResizeObserver;
  });

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        ZaakDetailsCardComponent,
        NoopAnimationsModule,
        TranslateModule.forRoot(),
      ],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        provideQueryClient(testQueryClient),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ZaakDetailsCardComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
  });

  describe("card header", () => {
    it("names the zaak by its identificatie and zaaktype", () => {
      renderCard();

      expect(screen().getByText("ZAAK-2026-0001")).toBeInTheDocument();
      expect(
        screen().getByText("fakeZaaktypeOmschrijving"),
      ).toBeInTheDocument();
    });

    it("shows the lock icon when the zaak is zaakspecifiek geautoriseerd", async () => {
      renderCard({ zaak: { ...zaak, isZaakspecifiekGeautoriseerd: true } });

      const lockIcon = await loader.getHarnessOrNull(
        MatIconHarness.with({ name: "lock" }),
      );

      expect(lockIcon).not.toBeNull();
    });

    it("labels the lock icon with the zaakspecifiek geautoriseerd translation", () => {
      renderCard({ zaak: { ...zaak, isZaakspecifiekGeautoriseerd: true } });

      expect(
        screen().getByTitle("zaak.isZaakspecifiekGeautoriseerd"),
      ).toBeInTheDocument();
    });

    it("does not show the lock icon when the zaak is not zaakspecifiek geautoriseerd", async () => {
      renderCard({ zaak: { ...zaak, isZaakspecifiekGeautoriseerd: false } });

      const lockIcon = await loader.getHarnessOrNull(
        MatIconHarness.with({ name: "lock" }),
      );

      expect(lockIcon).toBeNull();
    });
  });

  describe("conditional tabs", () => {
    it("shows only the always-present tabs for a bare zaak", async () => {
      renderCard();

      expect(await tabLabels()).toEqual(["gegevens.algemeen", "historie"]);
    });

    it("adds the locatie tab when the zaak has a zaakgeometrie", async () => {
      renderCard({
        zaak: {
          ...zaak,
          zaakgeometrie: fromPartial<GeneratedType<"RestGeometry">>({
            type: "POINT",
          }),
        },
      });

      expect(await tabLabels()).toContain("locatie");
    });

    it("adds the gerelateerdeZaken tab when the zaak has related zaken", async () => {
      renderCard({
        zaak: {
          ...zaak,
          gerelateerdeZaken: [
            fromPartial<GeneratedType<"RestGerelateerdeZaak">>({
              identificatie: "ZAAK-2026-0002",
              rechten: { lezen: true },
            }),
          ],
        },
      });

      expect(await tabLabels()).toContain("gerelateerdeZaken");
    });

    it("adds the betrokkenen tab when betrokkene koppelingen are configured", async () => {
      renderCard({ showBetrokkeneKoppelingen: true });

      expect(await tabLabels()).toContain("betrokkenen");
    });

    it("adds the bagObjecten tab when bag objecten are linked", async () => {
      renderCard({
        bagObjecten: [
          fromPartial<GeneratedType<"RESTBAGObjectGegevens">>({
            bagObject: fromPartial<GeneratedType<"RESTBAGObject">>({
              identificatie: "fakeBagIdentificatie",
              bagObjectType: "ADRES",
              omschrijving: "fakeBagOmschrijving",
            }),
          }),
        ],
      });

      expect(await tabLabels()).toContain("bagObjecten");
    });
  });
  describe("locatie tab", () => {
    const zaakOpLocatie = (wijzigenLocatie: boolean) => ({
      ...zaak,
      zaakgeometrie: fromPartial<GeneratedType<"RestGeometry">>({
        type: "POINT",
        point: { latitude: 52.1, longitude: 5.2 },
      }),
      rechten: { ...zaak.rechten, wijzigenLocatie },
    });

    it("shows the coordinates of the zaakgeometrie", async () => {
      renderCard({ zaak: zaakOpLocatie(false) });
      await openTab(/locatie/);

      expect(screen().getByText("52.1, 5.2")).toBeInTheDocument();
    });

    it("emits editLocationDetails when the user may edit the locatie", async () => {
      renderCard({ zaak: zaakOpLocatie(true) });
      await openTab(/locatie/);
      const editLocationDetails = jest.fn();
      fixture.componentInstance.editLocationDetails.subscribe(
        editLocationDetails,
      );

      locatieEditButton()!.click();

      expect(editLocationDetails).toHaveBeenCalled();
    });

    it("offers no edit button when the user may not edit the locatie", async () => {
      renderCard({ zaak: zaakOpLocatie(false) });
      await openTab(/locatie/);

      expect(locatieEditButton()).toBeNull();
    });
  });

  describe("wiring to the tab components", () => {
    it("re-emits editCaseDetails from the algemeen tab", async () => {
      const editCaseDetails = jest.fn();
      renderCard({
        zaak: { ...zaak, rechten: { ...zaak.rechten, wijzigen: true } },
      });
      fixture.componentInstance.editCaseDetails.subscribe(editCaseDetails);

      zaakEditButton()!.click();

      expect(editCaseDetails).toHaveBeenCalled();
    });

    it("re-emits zaakOntkoppelen from the gerelateerde zaken tab", async () => {
      const zaakOntkoppelen = jest.fn();
      const gerelateerdeZaak = fromPartial<
        GeneratedType<"RestGerelateerdeZaak">
      >({
        identificatie: "ZAAK-2026-0002",
        rechten: { lezen: true },
        ontkoppelen: true,
      });
      renderCard({ zaak: { ...zaak, gerelateerdeZaken: [gerelateerdeZaak] } });
      await openTab(/gerelateerdeZaken/);
      fixture.componentInstance.zaakOntkoppelen.subscribe(zaakOntkoppelen);

      screen().getByRole("button", { name: "actie.zaak.ontkoppelen" }).click();

      expect(zaakOntkoppelen).toHaveBeenCalledWith(gerelateerdeZaak);
    });

    it("re-emits bagObjectVerwijderen from the bag objecten tab", async () => {
      const bagObjectVerwijderen = jest.fn();
      const gekoppeldBagObject = fromPartial<
        GeneratedType<"RESTBAGObjectGegevens">
      >({
        bagObject: fromPartial<GeneratedType<"RESTBAGObject">>({
          identificatie: "fakeBagIdentificatie",
          bagObjectType: "ADRES",
          omschrijving: "fakeBagOmschrijving",
        }),
      });
      renderCard({ bagObjecten: [gekoppeldBagObject] });
      await openTab(/bagObjecten/);
      fixture.componentInstance.bagObjectVerwijderen.subscribe(
        bagObjectVerwijderen,
      );

      screen()
        .getByRole("button", { name: "actie.bagObject.ontkoppelen" })
        .click();

      expect(bagObjectVerwijderen).toHaveBeenCalledWith(gekoppeldBagObject);
    });
  });
});
