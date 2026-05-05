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
import { ZakenMijnComponent } from "./zaken-mijn.component";

describe(ZakenMijnComponent.name, () => {
  let component: ZakenMijnComponent;
  let fixture: ComponentFixture<ZakenMijnComponent>;
  let utilService: UtilService;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      imports: [
        ZakenMijnComponent,
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

    fixture = TestBed.createComponent(ZakenMijnComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  describe("ngOnInit", () => {
    it("sets the page title to title.zaken.mijn", () => {
      expect(utilService.setTitle).toHaveBeenCalledWith("title.zaken.mijn");
    });
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
