/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HarnessLoader } from "@angular/cdk/testing";
import { TestbedHarnessEnvironment } from "@angular/cdk/testing/testbed";
import { provideHttpClient } from "@angular/common/http";
import { provideHttpClientTesting } from "@angular/common/http/testing";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatPaginatorHarness } from "@angular/material/paginator/testing";
import { MatTableHarness } from "@angular/material/table/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { provideRouter } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { provideTanStackQuery } from "@tanstack/angular-query-experimental";
import { notifyManager } from "@tanstack/query-core";
import { of } from "rxjs";
import { fromPartial } from "src/test-helpers";
import { testQueryClient } from "../../../../setupJest";
import { WebsocketService } from "../../core/websocket/websocket.service";
import { SharedModule } from "../../shared/shared.module";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZakenMijnDatasource } from "../../zaken/zaken-mijn/zaken-mijn-datasource";
import { getDefaultZoekParameters } from "../../zoeken/model/zoek-parameters";
import { ZoekenService } from "../../zoeken/zoeken.service";
import { DashboardCard } from "../model/dashboard-card";
import { DashboardCardId } from "../model/dashboard-card-id";
import { DashboardCardType } from "../model/dashboard-card-type";
import { ZaakZoekenCardComponent } from "./zaak-zoeken-card.component";

const makeResultaat = (totaal: number, count = totaal) =>
  fromPartial<{
    resultaten: GeneratedType<"AbstractRestZoekObjectExtendsAbstractRestZoekObject">[];
    totaal: number;
  }>({
    resultaten: Array.from({ length: count }, (_, i) =>
      fromPartial<
        GeneratedType<"AbstractRestZoekObjectExtendsAbstractRestZoekObject">
      >({ identificatie: `ZAAK-${i}` }),
    ),
    totaal,
  });

const cardData = new DashboardCard(
  DashboardCardId.MIJN_ZAKEN,
  DashboardCardType.ZAAK_ZOEKEN,
);

function buildExpectedQueryKey() {
  const params = ZakenMijnDatasource.mijnLopendeZaken(
    getDefaultZoekParameters(),
  );
  params.sorteerVeld = "ZAAK_STARTDATUM";
  params.sorteerRichting = "desc";
  params.rows = 5;
  params.page = 0;
  return ["zaak zoeken dashboard", params];
}

describe(ZaakZoekenCardComponent.name, () => {
  let fixture: ComponentFixture<ZaakZoekenCardComponent>;
  let loader: HarnessLoader;
  let zoekenService: ZoekenService;

  beforeEach(async () => {
    notifyManager.setScheduler((fn) => fn());

    await TestBed.configureTestingModule({
      declarations: [ZaakZoekenCardComponent],
      imports: [SharedModule, NoopAnimationsModule, TranslateModule.forRoot()],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        provideTanStackQuery(testQueryClient),
        { provide: WebsocketService, useValue: { addListener: jest.fn() } },
      ],
    }).compileComponents();

    zoekenService = TestBed.inject(ZoekenService);
    jest.spyOn(zoekenService, "list").mockReturnValue(of(makeResultaat(0)));
  });

  afterEach(() => {
    notifyManager.setScheduler((fn) => Promise.resolve().then(fn));
    fixture?.destroy();
  });

  function createComponent() {
    fixture = TestBed.createComponent(ZaakZoekenCardComponent);
    fixture.componentInstance.data = cardData;
    loader = TestbedHarnessEnvironment.loader(fixture);
    fixture.detectChanges();
    // Stop the timed reload interval so harnesses (which await whenStable)
    // don't hang on a never-completing observable.
    fixture.componentInstance["reloader"]?.unsubscribe();
  }

  it("renders paginator length from the query result total even when one page of rows is loaded", async () => {
    testQueryClient.setQueryData(buildExpectedQueryKey(), makeResultaat(25, 5));

    createComponent();

    const paginator = await loader.getHarness(MatPaginatorHarness);
    expect(await paginator.getRangeLabel()).toContain("25");
  });

  it("does not bind the paginator to the dataSource so MatTableDataSource cannot overwrite paginator.length", () => {
    createComponent();

    expect(fixture.componentInstance.dataSource.paginator).toBeFalsy();
  });

  it("populates the data source with rows from the query result", () => {
    testQueryClient.setQueryData(buildExpectedQueryKey(), makeResultaat(4));

    createComponent();

    expect(fixture.componentInstance.dataSource.data).toHaveLength(4);
  });

  it("renders a table row for each result", async () => {
    testQueryClient.setQueryData(buildExpectedQueryKey(), makeResultaat(3));

    createComponent();

    const table = await loader.getHarness(MatTableHarness);
    expect((await table.getRows()).length).toBe(3);
  });

  it("declares the expected columns in display order", () => {
    createComponent();

    expect(fixture.componentInstance.columns).toEqual([
      "identificatie",
      "startdatum",
      "zaaktypeOmschrijving",
      "omschrijving",
      "url",
    ]);
  });

  it("updates pageNumber when onPageChange is called", () => {
    createComponent();

    fixture.componentInstance.onPageChange({ pageIndex: 2 });

    expect(fixture.componentInstance.pageNumber()).toBe(2);
  });

  it("propagates sort changes to zoekParameters and resets pagination", () => {
    createComponent();

    fixture.componentInstance.onPageChange({ pageIndex: 2 });
    expect(fixture.componentInstance.pageNumber()).toBe(2);

    fixture.componentInstance.sort!.sortChange.emit({
      active: "ZAAK_IDENTIFICATIE",
      direction: "asc",
    });

    expect(fixture.componentInstance.sortField()).toBe("ZAAK_IDENTIFICATIE");
    expect(fixture.componentInstance.sortDirection()).toBe("asc");
    expect(fixture.componentInstance.pageNumber()).toBe(0);
    expect(fixture.componentInstance.zoekParameters()).toMatchObject({
      sorteerVeld: "ZAAK_IDENTIFICATIE",
      sorteerRichting: "asc",
      page: 0,
    });
  });
});
