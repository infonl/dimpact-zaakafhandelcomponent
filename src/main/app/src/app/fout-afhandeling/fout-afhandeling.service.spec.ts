/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { TestBed } from "@angular/core/testing";
import { MatDialog } from "@angular/material/dialog";
import { firstValueFrom } from "rxjs";
import { UtilService } from "../core/service/util.service";
import { FoutAfhandelingService } from "./fout-afhandeling.service";

describe("FoutAfhandelingService", () => {
  let service: FoutAfhandelingService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        { provide: UtilService, useValue: {} },
        { provide: MatDialog, useValue: { open() {} } },
      ],
      imports: [],
    });

    service = TestBed.inject(FoutAfhandelingService);
  });

  it("should be created", () => {
    expect(service).toBeTruthy();
  });

  it("should return an observable error message when openFoutDialog is called", async () => {
    const error$ = service.openFoutDialog("some error");
    const errorMessage = await firstValueFrom(error$).catch((r) => r);
    expect(errorMessage).toEqual("Fout!");
  });
});
