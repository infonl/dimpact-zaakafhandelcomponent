/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HttpClientModule } from "@angular/common/http";
import { TestBed } from "@angular/core/testing";
import { FoutAfhandelingService } from "../fout-afhandeling/fout-afhandeling.service";
import { PlanItemsService } from "./plan-items.service";

describe("PlanItemServiceService", () => {
  let service: PlanItemsService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [{ provide: FoutAfhandelingService, useValue: {} }],
      imports: [HttpClientModule],
    });

    service = TestBed.inject(PlanItemsService);
  });

  it("should be created", () => {
    expect(service).toBeTruthy();
  });
});
