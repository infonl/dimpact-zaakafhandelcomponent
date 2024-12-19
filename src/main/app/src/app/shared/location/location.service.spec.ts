/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { TestBed } from "@angular/core/testing";

import { provideHttpClient, withInterceptorsFromDi } from "@angular/common/http";
import { FoutAfhandelingService } from "src/app/fout-afhandeling/fout-afhandeling.service";
import { LocationService } from "./location.service";

describe("LocationService", () => {
  let service: LocationService;

  beforeEach(() => {
    TestBed.configureTestingModule({
    imports: [],
    providers: [{ provide: FoutAfhandelingService, useValue: {} }, provideHttpClient(withInterceptorsFromDi())]
});

    service = TestBed.inject(LocationService);
  });
  it("should be created", () => {
    expect(service).toBeTruthy();
  });
});
