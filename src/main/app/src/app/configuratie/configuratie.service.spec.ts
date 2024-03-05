/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HttpClientModule } from "@angular/common/http";
import { TestBed } from "@angular/core/testing";
import { FoutAfhandelingService } from "../fout-afhandeling/fout-afhandeling.service";
import { ConfiguratieService } from "./configuratie.service";

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
