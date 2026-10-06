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
import { SignaleringenSettingsService } from "./signaleringen-settings.service";

describe(SignaleringenSettingsService.name, () => {
  let service: SignaleringenSettingsService;
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

    service = TestBed.inject(SignaleringenSettingsService);
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  describe("put", () => {
    it("puts the instellingen the user changed", async () => {
      const instellingen = fromPartial<
        GeneratedType<"RestSignaleringInstellingen">
      >({ id: 1, type: "ZAAK_OP_NAAM", dashboard: true, mail: false });

      runMutation(testQueryClient, service.put(), instellingen).subscribe();
      await sleep();

      const request = httpTestingController.expectOne(
        "/rest/signaleringen/instellingen",
      );
      expect(request.request.method).toBe("PUT");
      expect(request.request.body).toEqual(instellingen);
      request.flush(instellingen);
    });
  });
});
