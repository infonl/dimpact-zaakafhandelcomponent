/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  HttpErrorResponse,
  provideHttpClient,
  withInterceptorsFromDi,
} from "@angular/common/http";
import { provideHttpClientTesting } from "@angular/common/http/testing";
import { EventEmitter } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatPaginator, PageEvent } from "@angular/material/paginator";
import { MatSidenav } from "@angular/material/sidenav";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { provideTanStackQuery } from "@tanstack/angular-query-experimental";
import { screen } from "@testing-library/angular";
import { createQueryOptions, fromPartial } from "src/test-helpers";
import { sleep, testQueryClient } from "../../../../setupJest";
import { PolicyService } from "../../policy/policy.service";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZoekResultaat } from "../model/zoek-resultaat";
import { ZoekType } from "../model/zoek-type";
import { ZoekVeld } from "../model/zoek-veld";
import { ZoekenService } from "../zoeken.service";
import { ZoekComponent } from "./zoek.component";

describe(ZoekComponent.name, () => {
  const mockPaginator: Pick<
    MatPaginator,
    "page" | "pageIndex" | "pageSize" | "length"
  > = {
    page: new EventEmitter<PageEvent>(),
    pageIndex: 0,
    pageSize: 10,
    length: 0,
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [ZoekComponent, NoopAnimationsModule, TranslateModule.forRoot()],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        provideTanStackQuery(testQueryClient),
      ],
    });
  });

  describe("when it loads", () => {
    let list: jest.SpyInstance;

    async function load(sidenav: Pick<MatSidenav, "opened" | "openedStart">) {
      await TestBed.overrideComponent(ZoekComponent, {
        set: { template: "", imports: [] },
      }).compileComponents();
      list = jest.spyOn(TestBed.inject(ZoekenService), "list");
      list.mockReturnValue(
        createQueryOptions(
          fromPartial<
            ZoekResultaat<
              GeneratedType<"AbstractRestZoekObjectExtendsAbstractRestZoekObject">
            >
          >({ resultaten: [], totaal: 0, filters: {} }),
        ),
      );

      const fixture = TestBed.createComponent(ZoekComponent);
      Object.defineProperty(fixture.componentInstance, "paginator", {
        get: () => () => mockPaginator,
      });
      fixture.componentRef.setInput("zoekenSideNav", sidenav);
      fixture.componentInstance["trefwoordenControl"].setValue("fakeTrefwoord");
      fixture.detectChanges();
      await sleep();
    }

    it("searches when the sidenav was opened before it loaded", async () => {
      await load({ opened: true, openedStart: new EventEmitter<void>() });

      expect(list).toHaveBeenCalledTimes(1);
    });

    it("does not search while the sidenav is closed", async () => {
      await load({ opened: false, openedStart: new EventEmitter<void>() });

      expect(list).not.toHaveBeenCalled();
    });
  });

  describe("component logic", () => {
    let component: ZoekComponent;
    let fixture: ComponentFixture<ZoekComponent>;

    beforeEach(async () => {
      await TestBed.overrideComponent(ZoekComponent, {
        set: { template: "", imports: [] },
      }).compileComponents();

      fixture = TestBed.createComponent(ZoekComponent);
      component = fixture.componentInstance;

      Object.defineProperty(component, "paginator", {
        get: () => () => mockPaginator,
      });

      fixture.detectChanges();
    });

    describe("hasOption", () => {
      it("returns false for empty array", () => {
        expect(component["hasOption"]([])).toBe(false);
      });

      it("returns false when only option is -NULL-", () => {
        const options = [
          fromPartial<GeneratedType<"FilterResultaat">>({ naam: "-NULL-" }),
        ];
        expect(component["hasOption"](options)).toBe(false);
      });

      it("returns true when real options are present", () => {
        const options = [
          fromPartial<GeneratedType<"FilterResultaat">>({ naam: "ZAAK" }),
        ];
        expect(component["hasOption"](options)).toBe(true);
      });
    });

    describe("betrokkeneActief", () => {
      it("returns false when no betrokkene fields are set", () => {
        expect(component["betrokkeneActief"]()).toBe(false);
      });

      it("returns true when ZAAK_BETROKKENEN is set", () => {
        component["zoekParameters"].zoeken = { ZAAK_BETROKKENEN: "test" };
        expect(component["betrokkeneActief"]()).toBe(true);
      });

      it("returns true when ZAAK_INITIATOR is set", () => {
        component["zoekParameters"].zoeken = { ZAAK_INITIATOR: "test" };
        expect(component["betrokkeneActief"]()).toBe(true);
      });
    });

    describe("setZoektype", () => {
      it("enables trefwoordenControl when set to ZAC", () => {
        component["setZoektype"](ZoekType.PERSONEN);
        component["setZoektype"](ZoekType.ZAC);
        expect(component["trefwoordenControl"].enabled).toBe(true);
        expect(component["zoekType"]).toBe(ZoekType.ZAC);
      });

      it("disables trefwoordenControl when set to PERSONEN", () => {
        component["setZoektype"](ZoekType.PERSONEN);
        expect(component["trefwoordenControl"].disabled).toBe(true);
        expect(component["zoekType"]).toBe(ZoekType.PERSONEN);
      });

      it("disables trefwoordenControl when set to BEDRIJVEN", () => {
        component["setZoektype"](ZoekType.BEDRIJVEN);
        expect(component["trefwoordenControl"].disabled).toBe(true);
        expect(component["zoekType"]).toBe(ZoekType.BEDRIJVEN);
      });
    });

    describe("filterChanged", () => {
      it("sets filter value for a key", () => {
        const filterValue = fromPartial<GeneratedType<"FilterParameters">>({
          values: ["ZAAK"],
        });
        component["filterChanged"]("TYPE", filterValue);
        expect(component["zoekParameters"].filters?.["TYPE"]).toEqual(
          filterValue,
        );
      });

      it("removes filter when value is undefined", () => {
        const filterValue = fromPartial<GeneratedType<"FilterParameters">>({
          values: ["ZAAK"],
        });
        component["filterChanged"]("TYPE", filterValue);
        component["filterChanged"]("TYPE", undefined);
        expect(component["zoekParameters"].filters?.["TYPE"]).toBeUndefined();
      });
    });

    describe("dateFilterChange", () => {
      it("sets date range for a key", () => {
        const range = fromPartial<GeneratedType<"RestDatumRange">>({
          van: "2024-01-01",
          tot: "2024-12-31",
        });
        component["dateFilterChange"]("STARTDATUM", range);
        expect(component["zoekParameters"].datums?.["STARTDATUM"]).toEqual(
          range,
        );
      });
    });

    describe("reset", () => {
      it("resets search state", () => {
        component["hasSearched"] = true;
        component["hasTaken"] = true;
        component["hasZaken"] = true;
        component["hasDocument"] = true;
        component["reset"]();
        expect(component["hasSearched"]).toBe(false);
        expect(component["hasTaken"]).toBe(false);
        expect(component["hasZaken"]).toBe(false);
        expect(component["hasDocument"]).toBe(false);
      });

      it("resets trefwoordenControl value", () => {
        component["trefwoordenControl"].setValue("test");
        component["reset"]();
        expect(component["trefwoordenControl"].value).toBe("");
      });
    });

    describe("zoekVeldChanged", () => {
      it("updates huidigZoekVeld from zoekveldControl value", () => {
        component["zoekveldControl"].setValue(ZoekVeld.ZAAK_OMSCHRIJVING);
        component["zoekVeldChanged"]();
        expect(component["huidigZoekVeld"]).toBe(ZoekVeld.ZAAK_OMSCHRIJVING);
      });

      it("clears old zoekVeld from zoekParameters when changed", () => {
        component["zoekveldControl"].setValue(ZoekVeld.ZAAK_OMSCHRIJVING);
        component["huidigZoekVeld"] = ZoekVeld.ZAAK_OMSCHRIJVING;
        component["zoekParameters"].zoeken = { ZAAK_OMSCHRIJVING: "test" };
        component["zoekveldControl"].setValue(ZoekVeld.ALLE);
        component["zoekVeldChanged"]();
        expect(
          component["zoekParameters"].zoeken?.[ZoekVeld.ZAAK_OMSCHRIJVING],
        ).toBeUndefined();
      });
    });
    describe("a search that fails", () => {
      const zoekResultaat = fromPartial<
        ZoekResultaat<
          GeneratedType<"AbstractRestZoekObjectExtendsAbstractRestZoekObject">
        >
      >({ resultaten: [], totaal: 25, filters: {} });
      let list: jest.SpyInstance;

      async function search(pageIndex: number) {
        mockPaginator.pageIndex = pageIndex;
        mockPaginator.page.emit(fromPartial<PageEvent>({ pageIndex }));
        await sleep();
      }

      function failNextSearch() {
        list.mockReturnValue({
          queryKey: ["failing-query"],
          queryFn: jest
            .fn()
            .mockRejectedValue(new HttpErrorResponse({ status: 500 })),
        });
      }

      beforeEach(() => {
        mockPaginator.pageIndex = 0;
        list = jest.spyOn(TestBed.inject(ZoekenService), "list");
        list.mockReturnValue(createQueryOptions(zoekResultaat));
        component["trefwoordenControl"].setValue("fakeTrefwoord");
      });

      it("keeps the results and the page it is showing", async () => {
        await search(1);
        failNextSearch();

        await search(2);

        expect(mockPaginator.pageIndex).toBe(1);
        expect(component["zoekResultaat"]).toEqual(zoekResultaat);
      });

      it("goes back to the first page when the results were cleared before it", async () => {
        await search(2);
        component["trefwoordenControl"].setValue("");
        await search(0);
        component["trefwoordenControl"].setValue("fakeTrefwoord");
        failNextSearch();

        await search(1);

        expect(mockPaginator.pageIndex).toBe(0);
      });
    });
  });

  describe("personen button", () => {
    let fixture: ComponentFixture<ZoekComponent>;
    let policyService: PolicyService;

    beforeEach(async () => {
      policyService = TestBed.inject(PolicyService);

      testQueryClient.setQueryData(
        policyService.readBrpRechten().queryKey,
        fromPartial<GeneratedType<"RestBrpRechten">>({
          canZoeken: true,
        }),
      );

      fixture = TestBed.createComponent(ZoekComponent);
      Object.defineProperty(fixture.componentInstance, "paginator", {
        get: () => () => mockPaginator,
      });
      fixture.detectChanges();
    });

    describe("when brpZoeken is true", () => {
      it("should show the personen button", () => {
        expect(
          screen.getByRole("button", { name: "actie.zoeken.persoon" }),
        ).toBeVisible();
      });
    });

    describe("when brpZoeken is false", () => {
      beforeEach(() => {
        testQueryClient.setQueryData(
          policyService.readBrpRechten().queryKey,
          fromPartial<GeneratedType<"RestBrpRechten">>({
            canZoeken: false,
          }),
        );
        fixture = TestBed.createComponent(ZoekComponent);
        Object.defineProperty(fixture.componentInstance, "paginator", {
          get: () => () => mockPaginator,
        });
        fixture.detectChanges();
      });

      it("should hide the personen button", () => {
        expect(
          screen.queryByRole("button", { name: "actie.zoeken.persoon" }),
        ).not.toBeInTheDocument();
      });
    });
  });
});
