/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  provideHttpClient,
  withInterceptorsFromDi,
} from "@angular/common/http";
import { TestBed } from "@angular/core/testing";
import { TranslateModule } from "@ngx-translate/core";
import { of } from "rxjs";
import { ZacHttpClient } from "../shared/http/zac-http-client";
import {
  LINKABLE_ZAKEN_PAGINATION_SIZE,
  ZoekenService,
} from "./zoeken.service";

describe(ZoekenService.name, () => {
  let service: ZoekenService;
  let zacHttpClient: ZacHttpClient;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [TranslateModule.forRoot()],
      providers: [provideHttpClient(withInterceptorsFromDi())],
    });

    service = TestBed.inject(ZoekenService);
    zacHttpClient = TestBed.inject(ZacHttpClient);
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  describe("findLinkableZaken", () => {
    it("passes zoekZaakIdentifier, zoekZaakOmschrijving and relationType to the GET call", () => {
      jest.spyOn(zacHttpClient, "GET").mockReturnValue(of({} as never));

      service.findLinkableZaken({
        zaakUuid: "fakeZaakUuid",
        zoekZaakIdentifier: "fakeIdentifier",
        zoekZaakOmschrijving: "fakeOmschrijving",
        relationType: "HOOFDZAAK",
      });

      expect(zacHttpClient.GET).toHaveBeenCalledWith(
        "/rest/zaken/gekoppelde-zaken/{zaakUuid}/zoek-koppelbare-zaken",
        {
          path: { zaakUuid: "fakeZaakUuid" },
          query: {
            zoekZaakIdentifier: "fakeIdentifier",
            zoekZaakOmschrijving: "fakeOmschrijving",
            relationType: "HOOFDZAAK",
            page: 0,
            rows: LINKABLE_ZAKEN_PAGINATION_SIZE,
          },
        },
      );
    });
  });
});
