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
import { GeneratedType } from "../shared/utils/generated-types";
import { InboxDocumentenService } from "./inbox-documenten.service";

describe(InboxDocumentenService.name, () => {
  let service: InboxDocumentenService;
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

    service = TestBed.inject(InboxDocumentenService);
    utilService = TestBed.inject(UtilService);
    httpTestingController = TestBed.inject(HttpTestingController);
    jest.spyOn(utilService, "openSnackbar").mockImplementation(() => {});
  });

  afterEach(() => {
    httpTestingController.verify();
    testQueryClient.clear();
    jest.clearAllMocks();
  });

  describe("delete", () => {
    it("addresses the document by its id", async () => {
      const inboxDocument = fromPartial<GeneratedType<"RestInboxDocument">>({
        id: 42,
      });

      const request = service.delete().mutationFn!(
        inboxDocument,
        fromPartial<MutationFunctionContext>({}),
      );
      httpTestingController.expectOne("/rest/inboxdocumenten/42").flush(null);

      await request;
    });

    it("names the deleted document in the confirmation", async () => {
      const inboxDocument = fromPartial<GeneratedType<"RestInboxDocument">>({
        id: 42,
        titel: "fakeDocumentTitel",
      });

      await runMutationOnSuccess(service.delete(), inboxDocument);

      expect(utilService.openSnackbar).toHaveBeenCalledWith(
        "msg.document.verwijderen.uitgevoerd",
        { document: "fakeDocumentTitel" },
      );
    });
  });
});
