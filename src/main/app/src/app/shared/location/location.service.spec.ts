/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { TestBed } from "@angular/core/testing";

import { LocationService } from "./location.service";
import { FoutAfhandelingService } from "src/app/fout-afhandeling/fout-afhandeling.service";
import { HttpClientModule } from "@angular/common/http";

describe("LocationService", () => {
  let service: LocationService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [{ provide: FoutAfhandelingService, useValue: {} }],
      imports: [HttpClientModule],
    });

    service = TestBed.inject(LocationService);
  });
  it("should be created", () => {
    expect(service).toBeTruthy();
  });
});
