/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  provideHttpClient,
  withInterceptorsFromDi,
} from "@angular/common/http";
import { TestBed } from "@angular/core/testing";
import { TranslateModule } from "@ngx-translate/core";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { testQueryClient } from "../../../setupJest";
import { runMutationOnSuccess } from "../../test-helpers";
import { FoutAfhandelingService } from "../fout-afhandeling/fout-afhandeling.service";
import { TakenService } from "../taken/taken.service";
import { PlanItemsService } from "./plan-items.service";

describe("PlanItemServiceService", () => {
  let service: PlanItemsService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [TranslateModule.forRoot()],
      providers: [
        { provide: FoutAfhandelingService, useValue: {} },
        provideHttpClient(withInterceptorsFromDi()),
        provideQueryClient(testQueryClient),
      ],
    });

    service = TestBed.inject(PlanItemsService);
  });

  it("should be created", () => {
    expect(service).toBeTruthy();
  });

  describe("doHumanTaskPlanItem", () => {
    it("invalidates the taken listing of the zaak after the human task is started", async () => {
      const invalidateTakenVoorZaak = jest
        .spyOn(TestBed.inject(TakenService), "invalidateTakenVoorZaak")
        .mockResolvedValue(undefined);

      await runMutationOnSuccess(service.doHumanTaskPlanItem("zaak-uuid-1"));

      expect(invalidateTakenVoorZaak).toHaveBeenCalledWith("zaak-uuid-1");
    });
  });
});
