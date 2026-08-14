/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  provideHttpClient,
  withInterceptorsFromDi,
} from "@angular/common/http";
import { TestBed } from "@angular/core/testing";
import { TranslateService } from "@ngx-translate/core";
import {
  provideQueryClient,
  QueryClient,
} from "@tanstack/angular-query-experimental";
import { fromPartial } from "src/test-helpers";
import { FoutAfhandelingService } from "../fout-afhandeling/fout-afhandeling.service";
import { GeneratedType } from "../shared/utils/generated-types";
import { ZakenService } from "./zaken.service";

describe("ZaakService", () => {
  let service: ZakenService;
  let queryClient: QueryClient;

  beforeEach(() => {
    queryClient = new QueryClient();
    TestBed.configureTestingModule({
      imports: [],
      providers: [
        { provide: FoutAfhandelingService, useValue: {} },
        { provide: TranslateService, useValue: {} },
        provideHttpClient(withInterceptorsFromDi()),
        provideQueryClient(queryClient),
      ],
    });

    service = TestBed.inject(ZakenService);
  });

  it("should be created", () => {
    expect(service).toBeTruthy();
  });

  describe("readZaakQuery", () => {
    it("keys on the uuid endpoint so every caller shares one cache entry", () => {
      expect(service.readZaakQuery("fakeZaakUuid1").queryKey).toEqual([
        "/rest/zaken/zaak/{uuid}",
        { path: { uuid: "fakeZaakUuid1" } },
      ]);
    });
  });

  describe("cacheZaak", () => {
    it("writes the zaak into the cache entry for its own uuid", () => {
      const zaak = fromPartial<GeneratedType<"RestZaak">>({
        uuid: "fakeZaakUuid1",
        omschrijving: "fakeOmschrijving1",
      });

      service.cacheZaak(zaak);

      expect(
        queryClient.getQueryData(
          service.readZaakQuery("fakeZaakUuid1").queryKey,
        ),
      ).toBe(zaak);
    });
  });
});
