/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { provideHttpClient } from "@angular/common/http";
import { TestBed } from "@angular/core/testing";
import { TranslateModule } from "@ngx-translate/core";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { testQueryClient } from "../../../setupJest";
import { fromPartial, runMutationOnSuccess } from "../../test-helpers";
import { UtilService } from "../core/service/util.service";
import { GeneratedType } from "../shared/utils/generated-types";
import { InboxDocumentenService } from "./inbox-documenten.service";

describe(InboxDocumentenService.name, () => {
  let service: InboxDocumentenService;
  let utilService: UtilService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [TranslateModule.forRoot()],
      providers: [provideHttpClient(), provideQueryClient(testQueryClient)],
    });

    service = TestBed.inject(InboxDocumentenService);
    utilService = TestBed.inject(UtilService);
    jest.spyOn(utilService, "openSnackbar").mockImplementation(() => {});
  });

  afterEach(() => {
    testQueryClient.clear();
    jest.clearAllMocks();
  });

  describe("delete", () => {
    it("names the deleted document in the confirmation when it is not linked to a zaak", async () => {
      const inboxDocument = fromPartial<GeneratedType<"RestInboxDocument">>({
        id: 42,
        titel: "fakeDocumentTitel",
      });

      await runMutationOnSuccess(
        service.delete(inboxDocument),
        undefined,
        fromPartial<GeneratedType<"RestInboxDocumentDeleteResult">>({
          isGekoppeldAanZaak: false,
        }),
      );

      expect(utilService.openSnackbar).toHaveBeenCalledWith(
        "msg.document.verwijderen.uitgevoerd",
        { document: "fakeDocumentTitel" },
      );
    });

    it("warns that the document itself was not deleted when it is linked to a zaak", async () => {
      const inboxDocument = fromPartial<GeneratedType<"RestInboxDocument">>({
        id: 42,
        titel: "fakeDocumentTitel",
      });

      await runMutationOnSuccess(
        service.delete(inboxDocument),
        undefined,
        fromPartial<GeneratedType<"RestInboxDocumentDeleteResult">>({
          isGekoppeldAanZaak: true,
        }),
      );

      expect(utilService.openSnackbar).toHaveBeenCalledWith(
        "msg.document.verwijderen.inbox.gekoppeldAanZaak",
        { document: "fakeDocumentTitel" },
      );
    });
  });
});
