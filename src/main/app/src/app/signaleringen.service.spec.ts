/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { TestBed } from "@angular/core/testing";

import {
  provideHttpClient,
  withInterceptorsFromDi,
} from "@angular/common/http";
import { WebsocketService } from "./core/websocket/websocket.service";
import { FoutAfhandelingService } from "./fout-afhandeling/fout-afhandeling.service";
import { SignaleringenService } from "./signaleringen.service";

describe("SignaleringenService", () => {
  let service: SignaleringenService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [],
      providers: [
        { provide: FoutAfhandelingService, useValue: {} },
        { provide: WebsocketService, useValue: {} },
        provideHttpClient(withInterceptorsFromDi()),
      ],
    });
    service = TestBed.inject(SignaleringenService);
  });

  it("should be created", () => {
    expect(service).toBeTruthy();
  });
});
