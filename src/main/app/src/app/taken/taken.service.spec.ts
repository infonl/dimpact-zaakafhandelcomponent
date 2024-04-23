/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HttpClientModule } from "@angular/common/http";
import { TestBed } from "@angular/core/testing";
import { WebsocketService } from "../core/websocket/websocket.service";
import { FoutAfhandelingService } from "../fout-afhandeling/fout-afhandeling.service";
import { TakenService } from "./taken.service";

describe("TaakService", () => {
  let service;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        { provide: FoutAfhandelingService, useValue: {} },
        { provide: WebsocketService, useValue: {} },
      ],
      imports: [HttpClientModule],
    });

    service = TestBed.inject(TakenService);
  });

  it("should be created", () => {
    expect(service).toBeTruthy();
  });
});
