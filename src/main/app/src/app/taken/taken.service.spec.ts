/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { TakenService } from "./taken.service";
import { FoutAfhandelingService } from "../fout-afhandeling/fout-afhandeling.service";
import { TestBed } from "@angular/core/testing";
import { HttpClientModule } from "@angular/common/http";

describe("TaakService", () => {
  let service;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [{ provide: FoutAfhandelingService, useValue: {} }],
      imports: [HttpClientModule],
    });

    service = TestBed.inject(TakenService);
  });

  it("should be created", () => {
    expect(service).toBeTruthy();
  });
});
