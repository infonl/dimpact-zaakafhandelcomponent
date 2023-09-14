/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { ZakenService } from "./zaken.service";
import { FoutAfhandelingService } from "../fout-afhandeling/fout-afhandeling.service";
import { TestBed } from "@angular/core/testing";
import { HttpClientModule } from "@angular/common/http";

describe("ZaakService", () => {
  let service: ZakenService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [{ provide: FoutAfhandelingService, useValue: {} }],
      imports: [HttpClientModule],
    });

    service = TestBed.inject(ZakenService);
  });

  it("should be created", () => {
    expect(service).toBeTruthy();
  });
});
