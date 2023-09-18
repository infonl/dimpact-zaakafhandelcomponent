/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { TestBed } from "@angular/core/testing";

import { UtilService } from "./util.service";
import { TranslateService } from "@ngx-translate/core";
import { MatSnackBar } from "@angular/material/snack-bar";

describe("UtilService", () => {
  let service: UtilService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [{ provide: TranslateService, useValue: {} }, MatSnackBar],
      imports: [],
    });

    service = TestBed.inject(UtilService);
  });
  it("should be created", () => {
    expect(service).toBeTruthy();
  });
});
