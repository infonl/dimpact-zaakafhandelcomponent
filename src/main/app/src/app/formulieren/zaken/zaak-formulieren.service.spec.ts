/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { TestBed } from "@angular/core/testing";

import { TranslateService } from "@ngx-translate/core";
import { ZaakFormulierenService } from "./zaak-formulieren.service";

describe("ZaakFormulierenService", () => {
  let service: ZaakFormulierenService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        ZaakFormulierenService,
        { provide: TranslateService, useValue: {} },
      ],
    });
    service = TestBed.inject(ZaakFormulierenService);
  });

  it("should be created", () => {
    expect(service).toBeTruthy();
  });
});
