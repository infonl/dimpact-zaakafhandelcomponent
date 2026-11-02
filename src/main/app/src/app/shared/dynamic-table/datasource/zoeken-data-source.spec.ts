/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  HttpTestingController,
  TestRequest,
} from "@angular/common/http/testing";
import { TestBed } from "@angular/core/testing";
import { MatDialog } from "@angular/material/dialog";
import { MatPaginator } from "@angular/material/paginator";
import { MatSort } from "@angular/material/sort";
import { Router } from "@angular/router";
import { provideTranslateService } from "@ngx-translate/core";
import {
  fromPartial,
  provideZoekenServiceWithoutRetries,
} from "src/test-helpers";
import { sleep } from "../../../../../setupJest";
import { FoutDetailedDialogComponent } from "../../../fout-afhandeling/dialog/fout-detailed-dialog.component";
import { ZaakZoekObject } from "../../../zoeken/model/zaken/zaak-zoek-object";
import { ZoekParameters } from "../../../zoeken/model/zoek-parameters";
import { ZoekenService } from "../../../zoeken/zoeken.service";
import {
  SessionStorageUtil,
  WerklijstZoekParameter,
} from "../../storage/session-storage.util";
import { GeneratedType } from "../../utils/generated-types";
import { ZoekenDataSource } from "./zoeken-data-source";

const ZOEKEN_URL = "/rest/zoeken/list";
const SESSION_KEY =
  "WERKVOORRAAD_ZAKEN_ZOEKPARAMETERS" satisfies WerklijstZoekParameter;

class TestDataSource extends ZoekenDataSource<ZaakZoekObject> {
  constructor(zoekenService: ZoekenService) {
    super("WERKVOORRAAD_ZAKEN", zoekenService);
  }

  protected initZoekparameters(zoekParameters: ZoekParameters) {
    return { ...zoekParameters, type: "ZAAK" } satisfies ZoekParameters;
  }
}

describe(ZoekenDataSource.name, () => {
  const dialog = { open: jest.fn(), closeAll: jest.fn() };
  let dataSource: TestDataSource;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideTranslateService(),
        provideZoekenServiceWithoutRetries(),
        { provide: MatDialog, useValue: dialog },
      ],
    });
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  function createDataSource() {
    dataSource = TestBed.runInInjectionContext(
      () => new TestDataSource(TestBed.inject(ZoekenService)),
    );
  }

  async function settle() {
    TestBed.tick();
    await sleep();
    TestBed.tick();
    await sleep();
    TestBed.tick();
  }

  async function nextSearch() {
    await settle();
    return httpTestingController.expectOne(ZOEKEN_URL);
  }

  function searchedFor(request: TestRequest) {
    return request.request.body as ZoekParameters;
  }

  async function respondWith(
    request: TestRequest,
    resultaten: ZaakZoekObject[] = [],
    totaal = 75,
  ) {
    request.flush({ totaal, resultaten, filters: {} });
    await settle();
  }

  async function fail(request: TestRequest) {
    request.flush(
      { message: "fakeMessage", exception: "fakeException" },
      { status: 400, statusText: "Bad Request" },
    );
    await settle();
  }

  function storedZoekParameters() {
    return SessionStorageUtil.getItem<ZoekParameters>(SESSION_KEY);
  }

  const showTable = (
    paginator: MatPaginator,
    active = "",
  ): Promise<TestRequest> => {
    createDataSource();
    dataSource.setViewChilds(
      paginator,
      fromPartial<MatSort>({
        active,
        direction: active ? "asc" : "",
      }),
    );
    return nextSearch();
  };

  describe("given a table without an active sort", () => {
    it("leaves the sort field out of the search, so the backend does not reject an empty sort field", async () => {
      const request = await showTable(
        fromPartial<MatPaginator>({ pageIndex: 0, pageSize: 25 }),
      );

      expect(searchedFor(request).sorteerVeld).toBeUndefined();
      await respondWith(request);
    });
  });

  describe("given a table with an active sort", () => {
    it("searches on that sort field", async () => {
      const request = await showTable(
        fromPartial<MatPaginator>({ pageIndex: 0, pageSize: 25 }),
        "ZAAK_IDENTIFICATIE",
      );

      expect(searchedFor(request).sorteerVeld).toBe(
        "ZAAK_IDENTIFICATIE" satisfies GeneratedType<"SorteerVeld">,
      );
      await respondWith(request);
    });
  });

  it("starts from the search parameters of the previous visit", async () => {
    SessionStorageUtil.setItem(SESSION_KEY, {
      filters: { ZAAK_STATUS: { values: ["Open"], inverse: false } },
    });

    const request = await showTable(
      fromPartial<MatPaginator>({ pageIndex: 0, pageSize: 25 }),
    );

    expect(searchedFor(request).filters).toEqual({
      ZAAK_STATUS: { values: ["Open"], inverse: false },
    });
    await respondWith(request);
  });

  it("shows the rows of a search once it succeeds", async () => {
    const zaak = fromPartial<ZaakZoekObject>({ identificatie: "ZAAK-001" });

    await respondWith(
      await showTable(
        fromPartial<MatPaginator>({ pageIndex: 0, pageSize: 25 }),
      ),
      [zaak],
      1,
    );

    expect(dataSource.data).toEqual([zaak]);
    expect(dataSource.totalItems).toBe(1);
  });

  it("searches again after a delay, also when the search parameters did not change, so a bulk action shows", async () => {
    await respondWith(
      await showTable(
        fromPartial<MatPaginator>({ pageIndex: 0, pageSize: 25 }),
      ),
    );

    dataSource.load(20);
    await settle();
    httpTestingController.expectNone(ZOEKEN_URL);
    await sleep(30);

    await respondWith(await nextSearch());
  });

  describe("given a search for another page that fails", () => {
    const zaak = fromPartial<ZaakZoekObject>({ identificatie: "ZAAK-001" });
    let paginator: MatPaginator;

    beforeEach(async () => {
      paginator = fromPartial<MatPaginator>({ pageIndex: 1, pageSize: 25 });
      await respondWith(await showTable(paginator), [zaak]);

      paginator.pageIndex = 0;
      dataSource.zoekParameters.filters = {
        ZAAK_STATUS: { values: ["Open"], inverse: false },
      };
      dataSource.load();
      await fail(await nextSearch());
    });

    it("keeps the rows it is showing", () => {
      expect(dataSource.data).toEqual([zaak]);
    });

    it("puts the paginator back on the page of those rows", () => {
      expect(paginator.pageIndex).toBe(1);
    });

    it("remembers the page of those rows for the next visit", () => {
      expect(storedZoekParameters()?.page).toBe(1);
    });

    it("puts the filters back on those of the rows", () => {
      expect(dataSource.zoekParameters.filters).toEqual({});
    });

    it("remembers the filters of those rows for the next visit", () => {
      expect(storedZoekParameters()?.filters).toEqual({});
    });

    it("opens one dialog that says the previous rows and filters are shown again", () => {
      expect(dialog.open).toHaveBeenCalledTimes(1);
      expect(dialog.open).toHaveBeenCalledWith(FoutDetailedDialogComponent, {
        data: expect.objectContaining({
          waarschuwing: "msg.zoeken.mislukt.vorige.zoekopdracht",
          details: "fakeException",
        }),
      });
    });

    it("searches for that page again when it is asked for once more", async () => {
      paginator.pageIndex = 0;
      dataSource.load();
      const request = await nextSearch();

      expect(searchedFor(request).page).toBe(0);
      await respondWith(request);
    });
  });

  describe("given overlapping searches for different pages", () => {
    it("restores the paginator to the page of the rows when the later search fails", async () => {
      const firstPageZaak = fromPartial<ZaakZoekObject>({
        identificatie: "ZAAK-001",
      });
      const paginator = fromPartial<MatPaginator>({
        pageIndex: 1,
        pageSize: 25,
      });
      await respondWith(await showTable(paginator), [firstPageZaak]);

      paginator.pageIndex = 2;
      dataSource.load();
      const secondPageSearch = await nextSearch();
      paginator.pageIndex = 3;
      dataSource.load();
      const thirdPageSearch = await nextSearch();

      await respondWith(secondPageSearch, [
        fromPartial<ZaakZoekObject>({ identificatie: "ZAAK-051" }),
      ]);
      await fail(thirdPageSearch);

      expect(dataSource.data).toEqual([firstPageZaak]);
      expect(paginator.pageIndex).toBe(1);
      expect(storedZoekParameters()?.page).toBe(1);
    });
  });

  it("sends the user to the error page when a search fails because the session has expired", async () => {
    const navigate = jest
      .spyOn(TestBed.inject(Router), "navigate")
      .mockResolvedValue(true);
    const paginator = fromPartial<MatPaginator>({ pageIndex: 0, pageSize: 25 });
    await respondWith(await showTable(paginator));

    paginator.pageIndex = 1;
    dataSource.load();
    (await nextSearch()).error(new ProgressEvent("error"), { status: 0 });
    await settle();

    expect(navigate).toHaveBeenCalledWith(["/fout"]);
    expect(dialog.open).not.toHaveBeenCalled();
  });
});
