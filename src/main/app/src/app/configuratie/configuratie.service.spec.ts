/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { ConfiguratieService } from "./configuratie.service";
import { FoutAfhandelingService } from "../fout-afhandeling/fout-afhandeling.service";
import { TestBed } from "@angular/core/testing";
import { HttpClientModule } from "@angular/common/http";

describe("InformatieObjectService", () => {
  let service: ConfiguratieService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [{ provide: FoutAfhandelingService, useValue: {} }],
      imports: [HttpClientModule],
    });

    service = TestBed.inject(ConfiguratieService);
  });

  it("should be created", () => {
    expect(service).toBeTruthy();
  });
});
