/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { TestBed } from "@angular/core/testing";
import { GoogleMapsService } from "./google-maps.service";
import { HttpClientModule } from "@angular/common/http";
import { FoutAfhandelingService } from "src/app/fout-afhandeling/fout-afhandeling.service";
import {
  BUILDER_CONFIG,
  MaterialFormBuilderConfig,
} from "../material-form-builder-config";

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
