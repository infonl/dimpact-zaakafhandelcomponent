/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HttpClientModule } from "@angular/common/http";
import { TestBed } from "@angular/core/testing";
import { FoutAfhandelingService } from "src/app/fout-afhandeling/fout-afhandeling.service";
import { BUILDER_CONFIG } from "../material-form-builder-config";
import { GoogleMapsService } from "./google-maps.service";

describe("GoogleMapsServiceService", () => {
  let service: GoogleMapsService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        { provide: FoutAfhandelingService, useValue: {} },
        { provide: BUILDER_CONFIG, useValue: {} },
      ],
      imports: [HttpClientModule],
    });

    service = TestBed.inject(GoogleMapsService);
  });

  it("should be created", () => {
    expect(service).toBeTruthy();
  });
});
