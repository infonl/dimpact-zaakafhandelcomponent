/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { TestBed } from "@angular/core/testing";

import { HttpClientModule } from "@angular/common/http";
import { FoutAfhandelingService } from "./fout-afhandeling/fout-afhandeling.service";
import { SignaleringenService } from "./signaleringen.service";

describe("SignaleringenService", () => {
  let service: SignaleringenService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [{ provide: FoutAfhandelingService, useValue: {} }],
      imports: [HttpClientModule],
    });
    service = TestBed.inject(SignaleringenService);
  });

  it("should be created", () => {
    expect(service).toBeTruthy();
  });
});
