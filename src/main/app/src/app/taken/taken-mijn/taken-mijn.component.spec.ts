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
import { ZoekopdrachtComponent } from "../../gebruikersvoorkeuren/zoekopdracht/zoekopdracht.component";
import { ColumnPickerValue } from "../../shared/dynamic-table/column-picker/column-picker-value";
import { ColumnPickerComponent } from "../../shared/dynamic-table/column-picker/column-picker.component";
import { ZoekenColumn } from "../../shared/dynamic-table/model/zoeken-column";
import { ExportButtonComponent } from "../../shared/export-button/export-button.component";
import { MaterialModule } from "../../shared/material/material.module";
import { DagenPipe } from "../../shared/pipes/dagen.pipe";
import { DatumPipe } from "../../shared/pipes/datum.pipe";
import { EmptyPipe } from "../../shared/pipes/empty.pipe";
import { StaticTextComponent } from "../../shared/static-text/static-text.component";
import { DateRangeFilterComponent } from "../../shared/table-zoek-filters/date-range-filter/date-range-filter.component";
import { FacetFilterComponent } from "../../shared/table-zoek-filters/facet-filter/facet-filter.component";
import { TekstFilterComponent } from "../../shared/table-zoek-filters/tekst-filter/tekst-filter.component";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZoekenService } from "../../zoeken/zoeken.service";
import { TakenMijnComponent } from "./taken-mijn.component";

describe(TakenMijnComponent.name, () => {
  let component: TakenMijnComponent;
  let fixture: ComponentFixture<TakenMijnComponent>;
  let utilService: UtilService;
  let zoekenService: ZoekenService;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      imports: [
        TakenMijnComponent,
        MaterialModule,
        TranslateModule.forRoot(),
        NoopAnimationsModule,
        EmptyPipe,
        DatumPipe,
        DagenPipe,
        FacetFilterComponent,
        TekstFilterComponent,
        DateRangeFilterComponent,
        ZoekopdrachtComponent,
        ColumnPickerComponent,
        ExportButtonComponent,
        StaticTextComponent,
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

    zoekenService = TestBed.inject(ZoekenService);
    jest
      .spyOn(zoekenService, "list")
      .mockReturnValue(of({ resultaten: [], totaal: 0 }) as never);

    fixture = TestBed.createComponent(TakenMijnComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  describe("ngOnInit", () => {
    it("sets the page title to title.taken.mijn", () => {
      expect(utilService.setTitle).toHaveBeenCalledWith("title.taken.mijn");
    });
  });

  describe("getWerklijst", () => {
    it("returns WERKVOORRAAD_TAKEN", () => {
      expect(component["getWerklijst"]()).toBe("WERKVOORRAAD_TAKEN");
    });
  });

  describe("defaultColumns", () => {
    it("includes NAAM as VISIBLE", () => {
      const columns = component["defaultColumns"]();
      expect(columns.get(ZoekenColumn.NAAM)).toBe(ColumnPickerValue.VISIBLE);
    });

    it("includes URL as STICKY", () => {
      const columns = component["defaultColumns"]();
      expect(columns.get(ZoekenColumn.URL)).toBe(ColumnPickerValue.STICKY);
    });

    it("includes FATALEDATUM as VISIBLE", () => {
      const columns = component["defaultColumns"]();
      expect(columns.get(ZoekenColumn.FATALEDATUM)).toBe(
        ColumnPickerValue.VISIBLE,
      );
    });

    it("includes ZAAK_TOELICHTING as HIDDEN", () => {
      const columns = component["defaultColumns"]();
      expect(columns.get(ZoekenColumn.ZAAK_TOELICHTING)).toBe(
        ColumnPickerValue.HIDDEN,
      );
    });
  });

  describe("isAfterDate", () => {
    it("returns true for past dates", () => {
      const pastDate = new Date("2020-01-01");
      expect(component["isAfterDate"](pastDate)).toBe(true);
    });

    it("returns false for future dates", () => {
      const futureDate = new Date("2030-01-01");
      expect(component["isAfterDate"](futureDate)).toBe(false);
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
