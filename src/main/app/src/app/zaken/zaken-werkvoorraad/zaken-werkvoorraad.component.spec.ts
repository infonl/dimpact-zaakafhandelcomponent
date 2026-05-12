/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  provideHttpClient,
  withInterceptorsFromDi,
} from "@angular/common/http";
import { provideHttpClientTesting } from "@angular/common/http/testing";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { provideNativeDateAdapter } from "@angular/material/core";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { ActivatedRoute, Data, provideRouter } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { of } from "rxjs";
import { fromPartial } from "src/test-helpers";
import { testQueryClient } from "../../../../setupJest";
import { IdentityService } from "../../identity/identity.service";
import { TabelGegevens } from "../../shared/dynamic-table/model/tabel-gegevens";
import { ZoekenColumn } from "../../shared/dynamic-table/model/zoeken-column";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZaakZoekObject } from "../../zoeken/model/zaken/zaak-zoek-object";
import { ZakenWerkvoorraadComponent } from "./zaken-werkvoorraad.component";

describe(ZakenWerkvoorraadComponent.name, () => {
  let component: ZakenWerkvoorraadComponent;
  let fixture: ComponentFixture<ZakenWerkvoorraadComponent>;
  let identityService: IdentityService;

  const mockTabelGegevens: TabelGegevens = {
    aantalPerPagina: 10,
    pageSizeOptions: [10, 25, 50],
    werklijstRechten: fromPartial<GeneratedType<"RestWerklijstRechten">>({
      zakenTakenVerdelen: true,
      zakenTakenExporteren: true,
    }),
  };

  const mockRouteData: Data = {
    tabelGegevens: mockTabelGegevens,
  };

  const mockActivatedRoute = fromPartial<ActivatedRoute>({
    data: of(mockRouteData),
  });

  beforeEach(async () => {
    TestBed.configureTestingModule({
      imports: [
        ZakenWerkvoorraadComponent,
        NoopAnimationsModule,
        TranslateModule.forRoot(),
      ],
      providers: [
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: mockActivatedRoute,
        },
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        provideNativeDateAdapter(),
        provideQueryClient(testQueryClient),
      ],
    });

    fixture = TestBed.createComponent(ZakenWerkvoorraadComponent);
    component = fixture.componentInstance;

    identityService = TestBed.inject(IdentityService);

    testQueryClient.setQueryData(identityService.readLoggedInUser().queryKey, {
      id: "user1",
      naam: "testuser-1",
      groupIds: ["groupA", "groupB"],
    });
    fixture.detectChanges();
  });

  describe("showAssignToMe", () => {
    it.each([
      ["user2", true],
      ["user1", false],
    ])(
      "for user %s it should return %o",
      (user: string, expectation: boolean) => {
        const zaakZoekObject = fromPartial<ZaakZoekObject>({
          id: "zaak1",
          rechten: { toekennen: true },
          groepId: "groupA",
          behandelaarGebruikersnaam: user,
        });

        expect(component["showAssignToMe"](zaakZoekObject)).toBe(expectation);
      },
    );

    it("returns false when zaakZoekObject.rechten.toekennen is false", () => {
      const zaakZoekObject = fromPartial<ZaakZoekObject>({
        id: "zaak-no-assign",
        rechten: { toekennen: false },
        groepId: "groupA",
        behandelaarGebruikersnaam: "user2",
      });
      expect(component["showAssignToMe"](zaakZoekObject)).toBe(false);
    });

    it("returns false when loggedInUser is null", () => {
      testQueryClient.setQueryData(
        identityService.readLoggedInUser().queryKey,
        null,
      );
      const zaakZoekObject = fromPartial<ZaakZoekObject>({
        id: "zaak-no-user",
        rechten: { toekennen: true },
        groepId: "groupA",
        behandelaarGebruikersnaam: "user2",
      });
      expect(component["showAssignToMe"](zaakZoekObject)).toBe(false);
    });

    it("returns false when the user is not in the zaak's group", () => {
      const zaakZoekObject = fromPartial<ZaakZoekObject>({
        id: "zaak-other-group",
        rechten: { toekennen: true },
        groepId: "groupC",
        behandelaarGebruikersnaam: "user2",
      });
      expect(component["showAssignToMe"](zaakZoekObject)).toBe(false);
    });
  });

  describe("selection", () => {
    const mockZaak1 = fromPartial<ZaakZoekObject>({
      id: "zaak1",
      rechten: { toekennen: true },
      groepId: "groupA",
    });

    const mockZaak2 = fromPartial<ZaakZoekObject>({
      id: "zaak2",
      rechten: { toekennen: true },
      groepId: "groupA",
      behandelaarNaam: "Other User",
      behandelaarGebruikersnaam: "user2",
    });

    beforeEach(() => {
      jest
        .spyOn(component["dataSource"], "data", "get")
        .mockReturnValue([mockZaak1, mockZaak2]);
    });

    it("should initialize with empty selection", () => {
      expect(component["selection"].isEmpty()).toBe(true);
    });

    it("should return true when all rows are selected", () => {
      component["selection"].select(mockZaak1, mockZaak2);
      expect(component["isAllSelected"]()).toBe(true);
    });

    it("should return false when not all rows are selected", () => {
      component["selection"].select(mockZaak1);
      expect(component["isAllSelected"]()).toBe(false);
    });

    it("should select all rows when masterToggle is called and not all selected", () => {
      component["masterToggle"]();
      expect(component["selection"].selected.length).toBe(2);
    });

    it("should clear selection when masterToggle is called and all selected", () => {
      component["selection"].select(mockZaak1, mockZaak2);
      component["masterToggle"]();
      expect(component["selection"].isEmpty()).toBe(true);
    });

    it("should return correct checkbox label for row", () => {
      expect(component["checkboxLabel"](mockZaak1)).toBe("actie.selecteren");

      component["selection"].select(mockZaak1);
      expect(component["checkboxLabel"](mockZaak1)).toBe("actie.deselecteren");
    });

    it("should return correct checkbox label for header when none selected", () => {
      expect(component["checkboxLabel"]()).toBe("actie.alles.selecteren");
    });

    it("should return correct checkbox label for header when all selected", () => {
      component["selection"].select(mockZaak1, mockZaak2);
      expect(component["checkboxLabel"]()).toBe("actie.alles.deselecteren");
    });

    it("should return true for isSelected when items are selected", () => {
      component["selection"].select(mockZaak1);
      expect(component["isSelected"]()).toBe(true);
    });

    it("should return false for isSelected when no items are selected", () => {
      expect(component["isSelected"]()).toBe(false);
    });

    it("should count selected items correctly", () => {
      component["selection"].select(mockZaak1, mockZaak2);
      expect(component["countSelected"]()).toBe(2);
    });

    it("should count only zaken with behandelaar when checkIfZaakHasHandler is true", () => {
      component["selection"].select(mockZaak1, mockZaak2);
      expect(component["countSelected"](true)).toBe(1);
    });
  });

  describe("getWerklijst", () => {
    it("should return WERKVOORRAAD_ZAKEN", () => {
      expect(component["getWerklijst"]()).toBe("WERKVOORRAAD_ZAKEN");
    });
  });

  describe("isAfterDate", () => {
    it("should return true for past dates", () => {
      const pastDate = new Date("2020-01-01");
      expect(component["isAfterDate"](pastDate)).toBe(true);
    });

    it("should return false for future dates", () => {
      const futureDate = new Date("2030-01-01");
      expect(component["isAfterDate"](futureDate)).toBe(false);
    });
  });

  describe("defaultColumns", () => {
    it("should return default columns map", () => {
      const columns = component["defaultColumns"]();
      expect(columns).toBeInstanceOf(Map);
      expect(columns.size).toBeGreaterThan(0);
    });

    it("should include SELECT column when user has zakenTakenVerdelen rights", () => {
      const columns = component["defaultColumns"]();
      expect(columns.has(ZoekenColumn.SELECT)).toBe(true);
    });
  });
});
