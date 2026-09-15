/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { MatPaginator } from "@angular/material/paginator";
import { MatSort } from "@angular/material/sort";
import { of } from "rxjs";
import { fromPartial } from "src/test-helpers";
import { UtilService } from "../../../core/service/util.service";
import { ZaakZoekObject } from "../../../zoeken/model/zaken/zaak-zoek-object";
import { ZoekParameters } from "../../../zoeken/model/zoek-parameters";
import { ZoekenService } from "../../../zoeken/zoeken.service";
import { GeneratedType } from "../../utils/generated-types";
import { ZoekenDataSource } from "./zoeken-data-source";

class TestDataSource extends ZoekenDataSource<ZaakZoekObject> {
  constructor(zoekenService: ZoekenService, utilService: UtilService) {
    super("WERKVOORRAAD_ZAKEN", zoekenService, utilService);
  }

  protected initZoekparameters(zoekParameters: ZoekParameters) {
    return { ...zoekParameters, type: "ZAAK" } satisfies ZoekParameters;
  }
}

describe(ZoekenDataSource.name, () => {
  let list$: jest.Mock;
  let dataSource: TestDataSource;

  beforeEach(() => {
    jest.useFakeTimers();
    sessionStorage.clear();
    list$ = jest
      .fn()
      .mockReturnValue(of({ totaal: 0, resultaten: [], filters: {} }));
    dataSource = new TestDataSource(
      fromPartial<ZoekenService>({ list$ }),
      fromPartial<UtilService>({ setLoading: jest.fn() }),
    );
  });

  afterEach(() => {
    jest.useRealTimers();
  });

  const search = (active: string) => {
    dataSource.setViewChilds(
      fromPartial<MatPaginator>({ pageIndex: 0, pageSize: 25 }),
      fromPartial<MatSort>({
        active,
        direction: active ? "asc" : "",
      }),
    );
    jest.runAllTimers();
    return list$.mock.calls.at(-1)?.[0] as ZoekParameters;
  };

  describe("given a table without an active sort", () => {
    it("leaves the sort field out of the search, so the backend does not reject an empty sort field", () => {
      expect(search("").sorteerVeld).toBeUndefined();
    });
  });

  describe("given a table with an active sort", () => {
    it("searches on that sort field", () => {
      expect(search("ZAAK_IDENTIFICATIE").sorteerVeld).toBe(
        "ZAAK_IDENTIFICATIE" satisfies GeneratedType<"SorteerVeld">,
      );
    });
  });
});
