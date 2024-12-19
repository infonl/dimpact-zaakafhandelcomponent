/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HttpClient, provideHttpClient, withInterceptorsFromDi } from "@angular/common/http";
import { provideHttpClientTesting } from "@angular/common/http/testing";
import { TestBed } from "@angular/core/testing";
import { MatDialog } from "@angular/material/dialog";
import { Router } from "@angular/router";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { UtilService } from "../core/service/util.service";
import { FoutAfhandelingService } from "../fout-afhandeling/fout-afhandeling.service";
import { KlantenService } from "./klanten.service";

describe(KlantenService.name, () => {
  let service: KlantenService;
  let http: HttpClient;

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
    ]
});

    http = TestBed.inject(HttpClient);
    service = TestBed.inject(KlantenService);
  });

  describe(KlantenService.prototype.readBedrijf.name, () => {
    test.each([
      ["123456789", "rechtspersoon"],
      ["12345678", "vestiging"],
      ["1234567890", "vestiging"],
    ])(
      "for the rsinOfVestigingsnummer %i it should call the %s endpoint",
      (rsinOfVestigingsnummer, endpoint) => {
        const get = jest.spyOn(http, "get");

        service.readBedrijf(rsinOfVestigingsnummer);

        expect(get).toHaveBeenCalledTimes(1);
        expect(get).toHaveBeenCalledWith(
          expect.stringContaining(`${endpoint}/${rsinOfVestigingsnummer}`),
        );
      },
    );
  });
});
