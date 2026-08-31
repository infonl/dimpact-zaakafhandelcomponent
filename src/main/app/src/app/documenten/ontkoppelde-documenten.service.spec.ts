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
import { OntkoppeldeDocumentenService } from "./ontkoppelde-documenten.service";

describe(OntkoppeldeDocumentenService.name, () => {
  let service: OntkoppeldeDocumentenService;
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

    service = TestBed.inject(OntkoppeldeDocumentenService);
    utilService = TestBed.inject(UtilService);
    httpTestingController = TestBed.inject(HttpTestingController);
    jest.spyOn(utilService, "openSnackbar").mockImplementation(() => {});
  });

  describe("delete", () => {
    it("addresses the document by its id", async () => {
      const detachedDocument = fromPartial<
        GeneratedType<"RestDetachedDocument">
      >({ id: 42 });

      const request = service.delete().mutationFn!(
        detachedDocument,
        fromPartial<MutationFunctionContext>({}),
      );
      httpTestingController
        .expectOne("/rest/ontkoppeldedocumenten/42")
        .flush(null);

      await request;
    });

    it("names the deleted document in the confirmation", async () => {
      const detachedDocument = fromPartial<
        GeneratedType<"RestDetachedDocument">
      >({
        id: 42,
        titel: "fakeDocumentTitel",
      });

      await runMutationOnSuccess(service.delete(), detachedDocument);

      expect(utilService.openSnackbar).toHaveBeenCalledWith(
        "msg.document.verwijderen.uitgevoerd",
        { document: "fakeDocumentTitel" },
      );
    });
  });
});
