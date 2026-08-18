/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { provideHttpClient } from "@angular/common/http";
import {
  HttpTestingController,
  provideHttpClientTesting,
} from "@angular/common/http/testing";
import { TestBed } from "@angular/core/testing";
import { TranslateModule } from "@ngx-translate/core";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { sleep, testQueryClient } from "../../../setupJest";
import { fromPartial } from "../../test-helpers";
import { runMutation } from "../shared/http/run-mutation";
import { GeneratedType } from "../shared/utils/generated-types";
import { SignaleringenSettingsBeheerService } from "./signaleringen-settings-beheer.service";

describe(SignaleringenSettingsBeheerService.name, () => {
  let service: SignaleringenSettingsBeheerService;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [TranslateModule.forRoot()],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideQueryClient(testQueryClient),
      ],
    });

    service = TestBed.inject(SignaleringenSettingsBeheerService);
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  describe("put", () => {
    it("addresses the group by path and puts the changed instellingen", async () => {
      const instellingen = fromPartial<
        GeneratedType<"RestSignaleringInstellingen">
      >({ id: 1, type: "ZAAK_OP_NAAM", dashboard: true, mail: false });

      runMutation(
        testQueryClient,
        service.put("fakeGroupId"),
        instellingen,
      ).subscribe();
      await sleep();

      const request = httpTestingController.expectOne(
        "/rest/signaleringen/group/fakeGroupId/instellingen",
      );
      expect(request.request.method).toBe("PUT");
      expect(request.request.body).toEqual(instellingen);
      request.flush(instellingen);
    });
  });
});
