/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  provideHttpClient,
  withInterceptorsFromDi,
} from "@angular/common/http";
import {
  HttpTestingController,
  provideHttpClientTesting,
} from "@angular/common/http/testing";
import { TestBed } from "@angular/core/testing";
import { TranslateService } from "@ngx-translate/core";
import {
  type MutationFunctionContext,
  provideQueryClient,
} from "@tanstack/angular-query-experimental";
import { fromPartial } from "src/test-helpers";
import { testQueryClient } from "../../../setupJest";
import { FoutAfhandelingService } from "../fout-afhandeling/fout-afhandeling.service";
import { GeneratedType } from "../shared/utils/generated-types";
import { ZakenService } from "./zaken.service";

describe("ZaakService", () => {
  let service: ZakenService;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [],
      providers: [
        { provide: FoutAfhandelingService, useValue: {} },
        { provide: TranslateService, useValue: {} },
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        provideQueryClient(testQueryClient),
      ],
    });

    service = TestBed.inject(ZakenService);
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTestingController.verify();
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
        testQueryClient.getQueryData(
          service.readZaakQuery("fakeZaakUuid1").queryKey,
        ),
      ).toBe(zaak);
    });
  });

  describe("deleteInitiator", () => {
    it("addresses the zaak by its uuid and sends the reden as the body", async () => {
      const request = service.deleteInitiator().mutationFn!(
        { zaakUuid: "fakeZaakUuid", reden: "fakeReden" },
        fromPartial<MutationFunctionContext>({}),
      );
      const httpRequest = httpTestingController.expectOne(
        "/rest/zaken/fakeZaakUuid/initiator",
      );
      httpRequest.flush(null);

      expect(httpRequest.request.body).toEqual({ reden: "fakeReden" });
      await request;
    });
  });

  describe("deleteBetrokkene", () => {
    it("addresses the rol by its uuid and sends the reden as the body", async () => {
      const request = service.deleteBetrokkene().mutationFn!(
        { rolUuid: "fakeRolUuid", reden: "fakeReden" },
        fromPartial<MutationFunctionContext>({}),
      );
      const httpRequest = httpTestingController.expectOne(
        "/rest/zaken/betrokkene/fakeRolUuid",
      );
      httpRequest.flush(null);

      expect(httpRequest.request.body).toEqual({ reden: "fakeReden" });
      await request;
    });
  });
});
