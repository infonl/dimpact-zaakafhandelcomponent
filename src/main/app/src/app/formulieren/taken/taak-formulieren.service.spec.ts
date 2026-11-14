/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { TestBed } from "@angular/core/testing";

import {
  provideHttpClient,
  withInterceptorsFromDi,
} from "@angular/common/http";
import { TranslateService } from "@ngx-translate/core";
import { FoutAfhandelingService } from "src/app/fout-afhandeling/fout-afhandeling.service";
import { TaakFormulierenService } from "./taak-formulieren.service";

describe("TaakFormulierenService", () => {
  let service: TaakFormulierenService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [],
      providers: [
        { provide: FoutAfhandelingService, useValue: {} },
        { provide: TranslateService, useValue: {} },
        provideHttpClient(withInterceptorsFromDi()),
      ],
    });
    service = TestBed.inject(TaakFormulierenService);
  });

  it("should be created", () => {
    expect(service).toBeTruthy();
  });
});
