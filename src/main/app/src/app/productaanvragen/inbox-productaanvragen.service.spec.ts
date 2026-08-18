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
import {
  type MutationFunctionContext,
  provideQueryClient,
} from "@tanstack/angular-query-experimental";
import { testQueryClient } from "../../../setupJest";
import { fromPartial, runMutationOnSuccess } from "../../test-helpers";
import { UtilService } from "../core/service/util.service";
import { InboxProductaanvragenService } from "./inbox-productaanvragen.service";

describe(InboxProductaanvragenService.name, () => {
  let service: InboxProductaanvragenService;
  let utilService: UtilService;
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

    service = TestBed.inject(InboxProductaanvragenService);
    utilService = TestBed.inject(UtilService);
    httpTestingController = TestBed.inject(HttpTestingController);
    jest.spyOn(utilService, "openSnackbar").mockImplementation(() => {});
  });

  describe("delete", () => {
    it("addresses the productaanvraag by its id", async () => {
      const request = service.delete().mutationFn!(
        42,
        fromPartial<MutationFunctionContext>({}),
      );
      httpTestingController
        .expectOne("/rest/inbox-productaanvragen/42")
        .flush(null);

      await request;
    });

    it("confirms the deletion to the user", async () => {
      await runMutationOnSuccess(service.delete(), 42);

      expect(utilService.openSnackbar).toHaveBeenCalledWith(
        "msg.inboxProductaanvraag.verwijderen.uitgevoerd",
      );
    });
  });
});
