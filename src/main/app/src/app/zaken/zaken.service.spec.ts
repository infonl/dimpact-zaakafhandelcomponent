/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  provideHttpClient,
  withInterceptorsFromDi,
} from "@angular/common/http";
import { TestBed } from "@angular/core/testing";
import { TranslateService } from "@ngx-translate/core";
import { FoutAfhandelingService } from "../fout-afhandeling/fout-afhandeling.service";
import { ZakenService } from "./zaken.service";

describe("ZaakService", () => {
  let service: ZakenService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [],
      providers: [
        { provide: FoutAfhandelingService, useValue: {} },
        { provide: TranslateService, useValue: {} },
        provideHttpClient(withInterceptorsFromDi()),
      ],
    });

    service = TestBed.inject(ZakenService);
  });

  it("should be created", () => {
    expect(service).toBeTruthy();
  });
});
