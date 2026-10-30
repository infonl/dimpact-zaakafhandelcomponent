/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { TestBed } from "@angular/core/testing";

import {
  provideHttpClient,
  withInterceptorsFromDi,
  withXhr,
} from "@angular/common/http";
import { provideTranslateService } from "@ngx-translate/core";
import { WebsocketService } from "./core/websocket/websocket.service";
import { FoutAfhandelingService } from "./fout-afhandeling/fout-afhandeling.service";
import { SignaleringenService } from "./signaleringen.service";

describe("SignaleringenService", () => {
  let service: SignaleringenService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideTranslateService(),
        { provide: FoutAfhandelingService, useValue: {} },
        { provide: WebsocketService, useValue: {} },
        provideHttpClient(withXhr(), withInterceptorsFromDi()),
      ],
    });
    service = TestBed.inject(SignaleringenService);
  });

  it("should be created", () => {
    expect(service).toBeTruthy();
  });
});
