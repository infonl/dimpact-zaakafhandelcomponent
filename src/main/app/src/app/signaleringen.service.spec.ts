/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { TestBed } from "@angular/core/testing";

import { SignaleringenService } from "./signaleringen.service";
import { HttpClientModule } from "@angular/common/http";
import { FoutAfhandelingModule } from "./fout-afhandeling/fout-afhandeling.module";
import {
  TranslateLoader,
  TranslateModule,
  TranslateService,
  TranslateStore,
} from "@ngx-translate/core";
import { Translate } from "ol/interaction";
import { FoutAfhandelingService } from "./fout-afhandeling/fout-afhandeling.service";

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
