/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { provideHttpClient } from "@angular/common/http";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { provideMomentDateAdapter } from "@angular/material-moment-adapter";
import { MatDrawer } from "@angular/material/sidenav";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { provideRouter } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { of, throwError } from "rxjs";
import { DatumRange } from "src/app/zoeken/model/datum-range";
import { fromPartial } from "src/test-helpers";
import { UtilService } from "../../core/service/util.service";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZoekenService } from "../../zoeken/zoeken.service";
import { ZakenService } from "../zaken.service";
import { ZaakLinkComponent } from "./zaak-link.component";

const makeFakeZaak = (
  fields: Partial<GeneratedType<"RestZaak">> = {},
): GeneratedType<"RestZaak"> =>
  fromPartial<GeneratedType<"RestZaak">>({
    uuid: "fake-zaak-uuid",
    identificatie: "ZAAK-2026-001",
    ...fields,
  });

const makeFakeSearchResult = (
  fields: Partial<GeneratedType<"RestZaakKoppelenZoekObject">> = {},
): GeneratedType<"RestZaakKoppelenZoekObject"> =>
  fromPartial<GeneratedType<"RestZaakKoppelenZoekObject">>({
    id: "fake-result-uuid",
    identificatie: "ZAAK-2026-002",
    isKoppelbaar: true,
    ...fields,
  });

const makeFakeSideNav = (): MatDrawer =>
  fromPartial<MatDrawer>({ close: jest.fn().mockResolvedValue("close") });

const setup = (zaakFields: Partial<GeneratedType<"RestZaak">> = {}) => {
  const zaak = makeFakeZaak(zaakFields);
  const sideNav = makeFakeSideNav();

  TestBed.configureTestingModule({
    imports: [
      ZaakLinkComponent,
      NoopAnimationsModule,
      TranslateModule.forRoot(),
    ],
    providers: [
      provideHttpClient(),
      provideRouter([]),
      provideMomentDateAdapter(),
    ],
  });

  const zoekenService = TestBed.inject(ZoekenService);
  const zakenService = TestBed.inject(ZakenService);
  const utilService = TestBed.inject(UtilService);

  jest.spyOn(utilService, "setLoading").mockReturnValue(undefined);
  jest.spyOn(utilService, "openSnackbar").mockReturnValue(undefined);

  const fixture: ComponentFixture<ZaakLinkComponent> =
    TestBed.createComponent(ZaakLinkComponent);
  const component = fixture.componentInstance;
  component.zaak = zaak;
  component.sideNav = sideNav;
  fixture.detectChanges();

  return {
    fixture,
    component,
    zoekenService,
    zakenService,
    utilService,
    sideNav,
    zaak,
  };
};

describe(ZaakLinkComponent.name, () => {
  describe("form initialisation", () => {
    it("form is initially invalid", () => {
      const { component } = setup();
      expect(component["form"].valid).toBe(false);
    });
  });

  describe("caseRelationType valueChanges", () => {
    it("clears search results when caseRelationType changes", () => {
      const { component } = setup();
      component["cases"].data = [makeFakeSearchResult()];
      component["totalCases"] = 1;
      component["form"].controls.caseRelationType.setValue(
        component["caseRelationOptionsList"][0],
      );
      expect(component["cases"].data).toHaveLength(0);
      expect(component["totalCases"]).toBe(0);
    });
  });

  describe("searchCases()", () => {
    it("calls findLinkableZaken with correct parameters and sets results", () => {
      const { component, zoekenService, zaak } = setup();
      const resultRow = makeFakeSearchResult();
      const fakeResponse = fromPartial<
        GeneratedType<"RestZoekResultaatRestZaakKoppelenZoekObject">
      >({ resultaten: [resultRow], totaal: 1 });
      jest
        .spyOn(zoekenService, "findLinkableZaken")
        .mockReturnValue(
          of(fakeResponse) as ReturnType<ZoekenService["findLinkableZaken"]>,
        );

      component["form"].controls.caseRelationType.setValue(
        component["caseRelationOptionsList"][0],
      );
      component["form"].controls.caseNumberToSearchFor.setValue("ZAAK-2026");
      component["form"].controls.caseDescriptionToSearchFor.setValue(
        "ZAAKOMSCHR",
      );
      component["form"].controls.caseTypeToSearchFor.setValue(
        fromPartial<GeneratedType<"RestZaaktype">>({
          omschrijving: "ZAAKTYPEOMSCHR",
        }),
      );
      component["startdatum"] = new DatumRange(
        new Date(2026, 1, 1),
        new Date(2026, 2, 1),
      );
      component["einddatum"] = new DatumRange(
        new Date(2026, 3, 1),
        new Date(2026, 4, 1),
      );
      component["searchCases"]();

      expect(zoekenService.findLinkableZaken).toHaveBeenCalledWith({
        zaakUuid: zaak.uuid,
        zoekZaakIdentifier: "ZAAK-2026",
        zoekZaakOmschrijving: "ZAAKOMSCHR",
        zoekZaakTypeOmschrijving: "ZAAKTYPEOMSCHR",
        relationType: component["caseRelationOptionsList"][0].value,
        startdatum: {
          van: new Date(2026, 1, 1).toISOString(),
          tot: new Date(2026, 2, 1).toISOString(),
        },
        einddatum: {
          van: new Date(2026, 3, 1).toISOString(),
          tot: new Date(2026, 4, 1).toISOString(),
        },
      });
      expect(component["cases"].data).toEqual([resultRow]);
      expect(component["totalCases"]).toBe(1);
      expect(component["loading"]).toBe(false);
    });

    it("clears loading on error", () => {
      const { component, zoekenService } = setup();
      jest
        .spyOn(zoekenService, "findLinkableZaken")
        .mockReturnValue(
          throwError(() => new Error("server error")) as ReturnType<
            ZoekenService["findLinkableZaken"]
          >,
        );

      component["form"].controls.caseRelationType.setValue(
        component["caseRelationOptionsList"][0],
      );
      component["form"].controls.caseNumberToSearchFor.setValue("ZAAK-2026");
      component["searchCases"]();

      expect(component["loading"]).toBe(false);
    });
  });

  describe("selectCase()", () => {
    it("calls koppelZaak and emits zaakLinked on success", () => {
      const { component, zakenService, utilService } = setup();
      jest
        .spyOn(zakenService, "koppelZaak")
        .mockReturnValue(of(null) as ReturnType<ZakenService["koppelZaak"]>);
      const zaakLinkedSpy = jest.spyOn(component.zaakLinked, "emit");
      component["form"].controls.caseRelationType.setValue(
        component["caseRelationOptionsList"][0],
      );
      const row = makeFakeSearchResult({ isKoppelbaar: true });

      component["selectCase"](row);

      expect(zakenService.koppelZaak).toHaveBeenCalledWith({
        zaakUuid: component.zaak.uuid,
        teKoppelenZaakUuid: row.id,
        relatieType: component["caseRelationOptionsList"][0].value,
      });
      expect(zaakLinkedSpy).toHaveBeenCalled();
      expect(utilService.openSnackbar).toHaveBeenCalledWith(
        "msg.zaak.gekoppeld",
        {
          case: row.identificatie,
        },
      );
    });

    it("skips koppelZaak when row has no id", () => {
      const { component, zakenService } = setup();
      const koppelZaakSpy = jest
        .spyOn(zakenService, "koppelZaak")
        .mockReturnValue(of(null) as ReturnType<ZakenService["koppelZaak"]>);
      component["form"].controls.caseRelationType.setValue(
        component["caseRelationOptionsList"][0],
      );
      const rowWithoutId = makeFakeSearchResult({ id: undefined });

      component["selectCase"](rowWithoutId);

      expect(koppelZaakSpy).not.toHaveBeenCalled();
    });

    it("clears loading on error", () => {
      const { component, zakenService } = setup();
      jest
        .spyOn(zakenService, "koppelZaak")
        .mockReturnValue(
          throwError(() => new Error("fail")) as ReturnType<
            ZakenService["koppelZaak"]
          >,
        );
      component["form"].controls.caseRelationType.setValue(
        component["caseRelationOptionsList"][0],
      );
      component["loading"] = true;
      const row = makeFakeSearchResult({ isKoppelbaar: true });

      component["selectCase"](row);

      expect(component["loading"]).toBe(false);
    });

    it("skips koppelZaak when caseRelationType has no value selected", () => {
      const { component, zakenService } = setup();
      const koppelZaakSpy = jest
        .spyOn(zakenService, "koppelZaak")
        .mockReturnValue(of(null) as ReturnType<ZakenService["koppelZaak"]>);
      const row = makeFakeSearchResult({ isKoppelbaar: true });

      component["selectCase"](row);

      expect(koppelZaakSpy).not.toHaveBeenCalled();
    });
  });

  describe("rowDisabled()", () => {
    it("returns true when row is not koppelbaar", () => {
      const { component } = setup();
      const row = makeFakeSearchResult({
        isKoppelbaar: false,
        identificatie: "OTHER-ZAAK",
      });
      expect(component["rowDisabled"](row)).toBe(true);
    });

    it("returns true when row identificatie matches current zaak", () => {
      const { component, zaak } = setup();
      const row = makeFakeSearchResult({
        isKoppelbaar: true,
        identificatie: zaak.identificatie,
      });
      expect(component["rowDisabled"](row)).toBe(true);
    });

    it("returns false when row is koppelbaar and has different identificatie", () => {
      const { component } = setup();
      const row = makeFakeSearchResult({
        isKoppelbaar: true,
        identificatie: "OTHER-ZAAK-9999",
      });
      expect(component["rowDisabled"](row)).toBe(false);
    });
  });

  describe("close()", () => {
    it("calls sideNav.close() and resets form", () => {
      const { component, sideNav } = setup();
      component["form"].controls.caseRelationType.setValue(
        component["caseRelationOptionsList"][0],
      );
      component["close"]();
      expect(sideNav.close).toHaveBeenCalled();
      expect(component["form"].controls.caseRelationType.value).toBeNull();
    });
  });

  describe("reset()", () => {
    it("resets form and clears search results", () => {
      const { component } = setup();
      component["cases"].data = [makeFakeSearchResult()];
      component["totalCases"] = 5;
      component["form"].controls.caseRelationType.setValue(
        component["caseRelationOptionsList"][0],
      );

      component["reset"]();

      expect(component["form"].controls.caseRelationType.value).toBeNull();
      expect(component["cases"].data).toHaveLength(0);
      expect(component["totalCases"]).toBe(0);
    });
  });

  describe("clearSearchResult()", () => {
    it("clears data, totalCases and loading", () => {
      const { component, utilService } = setup();
      component["cases"].data = [makeFakeSearchResult()];
      component["totalCases"] = 3;
      component["loading"] = true;

      component["clearSearchResult"]();

      expect(component["cases"].data).toHaveLength(0);
      expect(component["totalCases"]).toBe(0);
      expect(component["loading"]).toBe(false);
      expect(utilService.setLoading).toHaveBeenCalledWith(false);
    });
  });

  describe("ngOnDestroy()", () => {
    it("completes the destroy subject without errors", () => {
      const { component } = setup();
      expect(() => component.ngOnDestroy()).not.toThrow();
    });
  });

  describe("caseRelationOptionsList", () => {
    it("contains an entry with value 'GERELATEERD'", () => {
      const { component } = setup();
      const gerelateerdeOption = component["caseRelationOptionsList"].find(
        (option) => option.value === "GERELATEERD",
      );
      expect(gerelateerdeOption).toBeDefined();
    });
  });

  describe("DOM behaviour", () => {
    it("search button is disabled when form is invalid", () => {
      const { fixture } = setup();
      const submitButton: HTMLButtonElement =
        fixture.nativeElement.querySelector('button[type="submit"]');
      expect(submitButton.disabled).toBe(true);
    });

    it("loading message is shown when loading is true", () => {
      const { component, fixture } = setup();
      component["loading"] = true;
      fixture.detectChanges();
      expect(fixture.nativeElement.textContent).toContain("msg.loading");
    });

    it("loading message is not shown when loading is false", () => {
      const { component, fixture } = setup();
      component["loading"] = false;
      fixture.detectChanges();
      expect(fixture.nativeElement.textContent).not.toContain("msg.loading");
    });

    it("cancel button calls close()", () => {
      const { fixture, component } = setup();
      const closeSpy = jest.spyOn(
        component as unknown as { close: () => unknown },
        "close",
      );
      const cancelButton: HTMLButtonElement = Array.from(
        fixture.nativeElement.querySelectorAll("button"),
      ).find((button: HTMLButtonElement) =>
        (button as HTMLButtonElement).textContent?.includes("actie.annuleren"),
      ) as HTMLButtonElement;
      cancelButton.click();
      expect(closeSpy).toHaveBeenCalled();
    });

    it("shows 'more than 10 results' message when results exceed 10", () => {
      const { component, fixture } = setup();
      component["loading"] = false;
      component["cases"].data = Array.from({ length: 11 }, () =>
        makeFakeSearchResult(),
      );
      component["totalCases"] = 11;
      fixture.detectChanges();
      expect(fixture.nativeElement.textContent).toContain(
        "msg.zaak.koppelem.meer-dan-10-gevonden",
      );
    });

    it("does not show 'more than 10 results' message when results are 10 or fewer", () => {
      const { component, fixture } = setup();
      component["loading"] = false;
      component["cases"].data = [makeFakeSearchResult()];
      component["totalCases"] = 1;
      fixture.detectChanges();
      expect(fixture.nativeElement.textContent).not.toContain(
        "msg.zaak.koppelem.meer-dan-10-gevonden",
      );
    });

    it("shows HOOFDZAAK label when caseRelationType is HOOFDZAAK", () => {
      const { component, fixture } = setup();
      component["form"].controls.caseRelationType.setValue(
        component["caseRelationOptionsList"].find(
          (option) => option.value === "HOOFDZAAK",
        )!,
      );
      fixture.detectChanges();
      expect(fixture.nativeElement.textContent).toContain(
        "zaak.koppelen.link.type.HOOFDZAAK",
      );
    });

    it("shows DEELZAAK label when caseRelationType is DEELZAAK", () => {
      const { component, fixture } = setup();
      component["form"].controls.caseRelationType.setValue(
        component["caseRelationOptionsList"].find(
          (option) => option.value === "DEELZAAK",
        )!,
      );
      fixture.detectChanges();
      expect(fixture.nativeElement.textContent).toContain(
        "zaak.koppelen.link.type.DEELZAAK",
      );
    });
  });
});
