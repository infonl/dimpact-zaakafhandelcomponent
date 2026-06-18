/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NgIf } from "@angular/common";
import {
  provideHttpClient,
  withInterceptorsFromDi,
} from "@angular/common/http";
import { provideHttpClientTesting } from "@angular/common/http/testing";
import { EventEmitter } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatPaginator, PageEvent } from "@angular/material/paginator";
import { By } from "@angular/platform-browser";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { provideTanStackQuery } from "@tanstack/angular-query-experimental";
import { fromPartial } from "src/test-helpers";
import { testQueryClient } from "../../../../setupJest";
import { PolicyService } from "../../policy/policy.service";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZoekType } from "../model/zoek-type";
import { ZoekVeld } from "../model/zoek-veld";
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
  });

  describe("personen button", () => {
    let fixture: ComponentFixture<ZoekComponent>;
    let policyService: PolicyService;

    beforeEach(async () => {

      policyService = TestBed.inject(PolicyService);

      testQueryClient.setQueryData(
        policyService.readBrpRechten().queryKey,
        fromPartial<GeneratedType<"RestBrpRechten">>({
          zoeken: true,
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
        const button = fixture.debugElement.query(By.css("#personen-button"));
        expect(button).not.toBeNull();
      });
    });

    describe("when brpZoeken is false", () => {
      beforeEach(() => {
        testQueryClient.setQueryData(
          policyService.readBrpRechten().queryKey,
          fromPartial<GeneratedType<"RestBrpRechten">>({
            zoeken: false,
          }),
        );
        fixture = TestBed.createComponent(ZoekComponent);
        Object.defineProperty(fixture.componentInstance, "paginator", {
          get: () => () => mockPaginator,
        });
        fixture.detectChanges();
      });

      it("should hide the personen button", () => {
        const button = fixture.debugElement.query(By.css("#personen-button"));
        expect(button).toBeNull();
      });
    });
  });
});
