/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  provideHttpClient,
  withInterceptorsFromDi,
  withXhr,
} from "@angular/common/http";
import { provideHttpClientTesting } from "@angular/common/http/testing";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { provideNativeDateAdapter } from "@angular/material/core";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { ActivatedRoute } from "@angular/router";
import { provideTranslateService } from "@ngx-translate/core";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { of } from "rxjs";
import {
  expectedSearchFor,
  fromPartial,
  searchThroughColumnFilter,
  WerklijstColumnFilter,
} from "src/test-helpers";
import { testQueryClient } from "../../../../setupJest";
import { ColumnPickerValue } from "../../shared/dynamic-table/column-picker/column-picker-value";
import { ZoekenColumn } from "../../shared/dynamic-table/model/zoeken-column";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZakenMijnComponent } from "./zaken-mijn.component";

describe(ZakenMijnComponent.name, () => {
  let component: ZakenMijnComponent;
  let fixture: ComponentFixture<ZakenMijnComponent>;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      imports: [ZakenMijnComponent, NoopAnimationsModule],
      providers: [
        provideTranslateService(),
        {
          provide: ActivatedRoute,
          useValue: {
            data: of({
              tabelGegevens: {
                aantalPerPagina: 10,
                pageSizeOptions: [10, 25, 50],
                werklijstRechten: fromPartial<
                  GeneratedType<"RestWerklijstRechten">
                >({}),
              },
            }),
          },
        },
        provideHttpClient(withXhr(), withInterceptorsFromDi()),
        provideHttpClientTesting(),
        provideQueryClient(testQueryClient),
        provideNativeDateAdapter(),
      ],
    });

    fixture = TestBed.createComponent(ZakenMijnComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  describe("getWerklijst", () => {
    it("returns MIJN_ZAKEN", () => {
      expect(component["getWerklijst"]()).toBe("MIJN_ZAKEN");
    });
  });

  describe("defaultColumns", () => {
    it("includes ZAAK_DOT_IDENTIFICATIE as VISIBLE", () => {
      const columns = component["defaultColumns"]();
      expect(columns.get(ZoekenColumn.ZAAK_DOT_IDENTIFICATIE)).toBe(
        ColumnPickerValue.VISIBLE,
      );
    });

    it("includes URL as STICKY", () => {
      const columns = component["defaultColumns"]();
      expect(columns.get(ZoekenColumn.URL)).toBe(ColumnPickerValue.STICKY);
    });

    it("includes STATUS as VISIBLE", () => {
      const columns = component["defaultColumns"]();
      expect(columns.get(ZoekenColumn.STATUS)).toBe(ColumnPickerValue.VISIBLE);
    });

    it("includes INDICATIES as VISIBLE", () => {
      const columns = component["defaultColumns"]();
      expect(columns.get(ZoekenColumn.INDICATIES)).toBe(
        ColumnPickerValue.VISIBLE,
      );
    });

    it("includes DAGEN_TOT_STREEFDATUM as VISIBLE", () => {
      const columns = component["defaultColumns"]();
      expect(columns.get(ZoekenColumn.DAGEN_TOT_STREEFDATUM)).toBe(
        ColumnPickerValue.VISIBLE,
      );
    });
  });

  describe("isAfterDate", () => {
    it("returns true for a past date", () => {
      expect(component["isAfterDate"]("2020-01-01")).toBe(true);
    });

    it("returns false for a future date", () => {
      expect(component["isAfterDate"]("2099-01-01")).toBe(false);
    });
  });

  describe("resetColumns", () => {
    it("delegates to dataSource.resetColumns", () => {
      jest
        .spyOn(component["dataSource"], "resetColumns")
        .mockReturnValue(undefined);
      component["resetColumns"]();
      expect(component["dataSource"].resetColumns).toHaveBeenCalled();
    });
  });
});

describe(`${ZakenMijnComponent.name} column filters`, () => {
  beforeEach(() => {
    sessionStorage.clear();
  });

  it.each<WerklijstColumnFilter>([
    { column: "zaak.identificatie", kind: "tekst", veld: "ZAAK_IDENTIFICATIE" },
    { column: "status", kind: "facet", veld: "ZAAK_STATUS" },
    { column: "zaaktype", kind: "facet", veld: "ZAAKTYPE" },
    { column: "omschrijving", kind: "tekst", veld: "ZAAK_OMSCHRIJVING" },
    { column: "groep", kind: "facet", veld: "GROEP" },
    {
      column: "vertrouwelijkheidaanduiding",
      kind: "facet",
      veld: "ZAAK_VERTROUWELIJKHEIDAANDUIDING",
    },
    {
      column: "communicatiekanaal",
      kind: "facet",
      veld: "ZAAK_COMMUNICATIEKANAAL",
    },
    {
      column: "registratiedatum",
      kind: "datum",
      veld: "ZAAK_REGISTRATIEDATUM",
    },
    { column: "startdatum", kind: "datum", veld: "ZAAK_STARTDATUM" },
    { column: "einddatum", kind: "datum", veld: "ZAAK_EINDDATUM" },
    { column: "einddatum-gepland", kind: "datum", veld: "ZAAK_STREEFDATUM" },
    {
      column: "uiterlijke-einddatum-afdoening",
      kind: "datum",
      veld: "ZAAK_FATALE_DATUM",
    },
    { column: "toelichting", kind: "tekst", veld: "ZAAK_TOELICHTING" },
    { column: "indicaties", kind: "facet", veld: "ZAAK_INDICATIES" },
  ])("searches on what was put in the $column filter", async (filter) => {
    expect(
      await searchThroughColumnFilter(ZakenMijnComponent, filter),
    ).toMatchObject(expectedSearchFor(filter));
  });
});
