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
import { KlantenService } from "./klanten.service";
import { GeneratedType } from "../shared/utils/generated-types";

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
    const baseIdentificatie = {
      kvkNummer: null,
      vestigingsnummer: null,
      rsin: null,
      bsnNummer: null,
    };

    test.each([
      [
        {
          ...baseIdentificatie,
          type: "VN",
          kvkNummer: "12345678",
          vestigingsnummer: "12345678",
        },
        "vestiging",
        { kvkNummer: "12345678", vestigingsnummer: "12345678" },
      ],
      [
        // legacy
        { ...baseIdentificatie, type: "VN", vestigingsnummer: "1234567890" },
        "vestiging",
        { vestigingsnummer: "1234567890" },
      ],
      [
        {
          ...baseIdentificatie,
          type: "RSIN",
          kvkNummer: "12345678",
        },
        "rechtspersoon",
        { kvkNummer: "12345678" },
      ],
      [
        // legacy
        { ...baseIdentificatie, type: "RSIN", rsin: "123456789" },
        "rechtspersoon",
        { rsin: "123456789" },
      ],
    ])(
      "for betrokkeneIdentificatie %o it should call the %s endpoint",
      (betrokkeneIdentificatie, endpoint, path) => {
        const get = jest.spyOn(zacHttpClient, "GET");

        service.readBedrijf(
          betrokkeneIdentificatie as GeneratedType<"BetrokkeneIdentificatie">,
        );

        expect(get).toHaveBeenCalledWith(expect.stringContaining(endpoint), {
          path,
        });
      },
    );
  });
});
