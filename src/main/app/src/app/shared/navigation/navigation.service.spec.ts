/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Location } from "@angular/common";
import { TestBed } from "@angular/core/testing";
import { RouterTestingModule } from "@angular/router/testing";
import { UtilService } from "../../core/service/util.service";
import { NavigationService } from "./navigation.service";

describe("NavigationService", () => {
  let service: NavigationService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        Location,
        UtilService,
        { provide: UtilService, useValue: {} },
      ],
      imports: [RouterTestingModule.withRoutes([])],
    }).compileComponents();
    service = TestBed.inject(NavigationService);
  });

  it("should be created", () => {
    expect(service).toBeTruthy();
  });
});
