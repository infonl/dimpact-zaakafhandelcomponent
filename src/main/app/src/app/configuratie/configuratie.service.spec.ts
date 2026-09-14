/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  provideHttpClient,
  withInterceptorsFromDi,
} from "@angular/common/http";
import { TestBed } from "@angular/core/testing";
import { TranslateModule } from "@ngx-translate/core";
import { FoutAfhandelingService } from "../fout-afhandeling/fout-afhandeling.service";
import { ConfiguratieService } from "./configuratie.service";

describe("InformatieObjectService", () => {
  let service: ConfiguratieService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [TranslateModule.forRoot()],
      providers: [
        { provide: FoutAfhandelingService, useValue: {} },
        provideHttpClient(withInterceptorsFromDi()),
      ],
    });

    service = TestBed.inject(ConfiguratieService);
  });

  it("should be created", () => {
    expect(service).toBeTruthy();
  });
});
