/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
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
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { firstValueFrom } from "rxjs";
import { testQueryClient } from "../../../setupJest";
import { FoutAfhandelingService } from "../fout-afhandeling/fout-afhandeling.service";
import { runMutation } from "../shared/http/run-mutation";
import { ZakenService } from "../zaken/zaken.service";
import { BAGService } from "./bag.service";

describe(BAGService.name, () => {
  let service: BAGService;
  let zakenService: ZakenService;
  let httpTestingController: HttpTestingController;

  const flushDelete = async () => {
    await new Promise(requestAnimationFrame);
    httpTestingController.expectOne("/rest/bag").flush(null);
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        { provide: FoutAfhandelingService, useValue: {} },
        { provide: TranslateService, useValue: {} },
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        provideQueryClient(testQueryClient),
      ],
    });

    service = TestBed.inject(BAGService);
    zakenService = TestBed.inject(ZakenService);
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe("delete", () => {
    it("invalidates the historie of the zaak the bag object was coupled to, so every caller gets a fresh one", async () => {
      const invalidateQueries = jest.spyOn(
        testQueryClient,
        "invalidateQueries",
      );

      const deleted = firstValueFrom(
        runMutation(testQueryClient, service.delete(), {
          uuid: "fakeBagObjectGegevensUuid",
          zaakUuid: "fakeZaakUuid",
          redenWijzigen: "fakeReden",
        }),
      );
      await flushDelete();
      await deleted;

      expect(invalidateQueries).toHaveBeenCalledWith(
        {
          queryKey:
            zakenService.listHistorieVoorZaakQuery("fakeZaakUuid").queryKey,
        },
        { cancelRefetch: false },
      );
    });

    it("invalidates no historie when the request carries no zaak uuid, which the endpoint types allow", async () => {
      const invalidateHistorie = jest.spyOn(zakenService, "invalidateHistorie");

      const deleted = firstValueFrom(
        runMutation(testQueryClient, service.delete(), {
          uuid: "fakeBagObjectGegevensUuid",
          redenWijzigen: "fakeReden",
        }),
      );
      await flushDelete();
      await deleted;

      expect(invalidateHistorie).not.toHaveBeenCalled();
    });
  });
});
