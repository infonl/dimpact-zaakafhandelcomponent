/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  provideHttpClient,
  withInterceptorsFromDi,
} from "@angular/common/http";
import {
  HttpTestingController,
  provideHttpClientTesting,
} from "@angular/common/http/testing";
import { TestBed } from "@angular/core/testing";
import { TranslateModule } from "@ngx-translate/core";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { sleep, testQueryClient } from "../../../setupJest";
import { fromPartial, runMutationOnSuccess } from "../../test-helpers";
import { FoutAfhandelingService } from "../fout-afhandeling/fout-afhandeling.service";
import { PostBody } from "../shared/http/http-client";
import { runMutation } from "../shared/http/run-mutation";
import { TakenService } from "../taken/taken.service";
import { PlanItemsService } from "./plan-items.service";

describe("PlanItemServiceService", () => {
  let service: PlanItemsService;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [TranslateModule.forRoot()],
      providers: [
        { provide: FoutAfhandelingService, useValue: {} },
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        provideQueryClient(testQueryClient),
      ],
    });

    service = TestBed.inject(PlanItemsService);
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTestingController.verify();
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

  describe("doUserEventListenerPlanItem", () => {
    it("posts the user event listener plan item", async () => {
      const userEventListenerData = fromPartial<
        PostBody<"/rest/planitems/doUserEventListenerPlanItem">
      >({
        actie: "INTAKE_AFRONDEN",
        planItemInstanceId: "fakePlanItemId",
        zaakUuid: "fakeZaakUuid",
      });

      runMutation(
        testQueryClient,
        service.doUserEventListenerPlanItem(),
        userEventListenerData,
      ).subscribe();
      await sleep();

      const request = httpTestingController.expectOne(
        "/rest/planitems/doUserEventListenerPlanItem",
      );
      expect(request.request.method).toBe("POST");
      expect(request.request.body).toEqual(userEventListenerData);
      request.flush(null);
    });
  });
});
