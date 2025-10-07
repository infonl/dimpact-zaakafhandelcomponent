/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  provideHttpClient,
  withInterceptorsFromDi,
} from "@angular/common/http";
import { provideHttpClientTesting } from "@angular/common/http/testing";
import { TestBed } from "@angular/core/testing";
import { MatDialog } from "@angular/material/dialog";
import { Router } from "@angular/router";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { UtilService } from "../core/service/util.service";
import { FoutAfhandelingService } from "../fout-afhandeling/fout-afhandeling.service";
import { ZacHttpClient } from "../shared/http/zac-http-client";
import { BetrokkeneIdentificatie } from "../zaken/model/betrokkeneIdentificatie";
import { KlantenService } from "./klanten.service";

describe(KlantenService.name, () => {
  let service: KlantenService;
  let zacHttpClient: ZacHttpClient;

  const mockTranslateService = {
    instant: (arg: string) => {
      return arg;
    },
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [TranslateModule.forRoot()],
      providers: [
        Router,
        MatDialog,
        UtilService,
        { provide: TranslateService, useValue: mockTranslateService },
        FoutAfhandelingService,
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
      ],
    });

    zacHttpClient = TestBed.inject(ZacHttpClient);
    service = TestBed.inject(KlantenService);
  });

  describe(KlantenService.prototype.readBedrijf.name, () => {
    test.each([
      [
        new BetrokkeneIdentificatie({
          identificatieType: "VN",
          kvkNummer: "12345678",
          vestigingsnummer: "12345678",
        }),
        "vestiging",
        { kvkNummer: "12345678", vestigingsnummer: "12345678" },
      ],
      [
        new BetrokkeneIdentificatie({
          identificatieType: "VN",
          kvkNummer: null,
          vestigingsnummer: "12345678",
        }),
        "vestiging",
        { vestigingsnummer: "12345678" },
      ],
      [
        new BetrokkeneIdentificatie({
          identificatieType: "RSIN",
          kvkNummer: "12345678",
        }),
        "rechtspersoon",
        { kvkNummer: "12345678" },
      ],
      [
        // legacy
        new BetrokkeneIdentificatie({
          identificatieType: "RSIN",
          rsin: "123456789",
        }),
        "rechtspersoon",
        { rsin: "123456789" },
      ],
    ])(
      "for betrokkeneIdentificatie %o it should call the %s endpoint",
      (betrokkeneIdentificatie, endpoint, path) => {
        const get = jest.spyOn(zacHttpClient, "GET");

        service.readBedrijf(betrokkeneIdentificatie);

        expect(get).toHaveBeenCalledWith(expect.stringContaining(endpoint), {
          path,
        });
      },
    );
  });
});
