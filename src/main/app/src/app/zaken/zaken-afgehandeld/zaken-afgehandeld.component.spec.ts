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
import { ActivatedRoute } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { of } from "rxjs";
import { fromPartial } from "src/test-helpers";
import { testQueryClient } from "../../../../setupJest";
import { UtilService } from "../../core/service/util.service";
import { ColumnPickerValue } from "../../shared/dynamic-table/column-picker/column-picker-value";
import { ZoekenColumn } from "../../shared/dynamic-table/model/zoeken-column";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZakenAfgehandeldComponent } from "./zaken-afgehandeld.component";

describe(ZakenAfgehandeldComponent.name, () => {
  let component: ZakenAfgehandeldComponent;
  let fixture: ComponentFixture<ZakenAfgehandeldComponent>;
  let utilService: UtilService;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      imports: [
        ZakenAfgehandeldComponent,
        TranslateModule.forRoot(),
        NoopAnimationsModule,
      ],
      providers: [
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
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        provideQueryClient(testQueryClient),
        provideNativeDateAdapter(),
      ],
    });

    utilService = TestBed.inject(UtilService);
    jest.spyOn(utilService, "setTitle").mockReturnValue(undefined);

    fixture = TestBed.createComponent(ZakenAfgehandeldComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  describe("ngOnInit", () => {
    it("sets the page title to title.zaken.afgehandeld", () => {
      expect(utilService.setTitle).toHaveBeenCalledWith(
        "title.zaken.afgehandeld",
      );
    });
  });

  describe("getWerklijst", () => {
    it("returns AFGEHANDELDE_ZAKEN", () => {
      expect(component["getWerklijst"]()).toBe("AFGEHANDELDE_ZAKEN");
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

    it("includes EINDDATUM as VISIBLE", () => {
      const columns = component["defaultColumns"]();
      expect(columns.get(ZoekenColumn.EINDDATUM)).toBe(
        ColumnPickerValue.VISIBLE,
      );
    });

    it("includes BEHANDELAAR as VISIBLE", () => {
      const columns = component["defaultColumns"]();
      expect(columns.get(ZoekenColumn.BEHANDELAAR)).toBe(
        ColumnPickerValue.VISIBLE,
      );
    });

    it("includes RESULTAAT as VISIBLE", () => {
      const columns = component["defaultColumns"]();
      expect(columns.get(ZoekenColumn.RESULTAAT)).toBe(
        ColumnPickerValue.VISIBLE,
      );
    });

    it("includes STATUS as HIDDEN", () => {
      const columns = component["defaultColumns"]();
      expect(columns.get(ZoekenColumn.STATUS)).toBe(ColumnPickerValue.HIDDEN);
    });

    it("includes INDICATIES as HIDDEN", () => {
      const columns = component["defaultColumns"]();
      expect(columns.get(ZoekenColumn.INDICATIES)).toBe(
        ColumnPickerValue.HIDDEN,
      );
    });
  });

  describe("isAfterDateLimit", () => {
    it("returns true when date exceeds the limit", () => {
      const exceededDate = "2020-01-01";
      const limitDate = "2021-01-01";
      expect(component["isAfterDateLimit"](exceededDate, limitDate)).toBe(true);
    });

    it("returns false when date does not exceed the limit", () => {
      const futureDate = "2030-01-01";
      const limitDate = "2020-01-01";
      expect(component["isAfterDateLimit"](futureDate, limitDate)).toBe(false);
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

  describe("filtersChange", () => {
    it("delegates to dataSource.filtersChanged", () => {
      jest
        .spyOn(component["dataSource"], "filtersChanged")
        .mockReturnValue(undefined);
      component["filtersChange"]();
      expect(component["dataSource"].filtersChanged).toHaveBeenCalled();
    });
  });
});
