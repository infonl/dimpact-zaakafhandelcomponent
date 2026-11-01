/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { provideHttpClient, withXhr } from "@angular/common/http";
import {
  HttpTestingController,
  provideHttpClientTesting,
} from "@angular/common/http/testing";
import { TestBed } from "@angular/core/testing";
import { MatDialog, MatDialogRef } from "@angular/material/dialog";
import { provideTranslateService } from "@ngx-translate/core";
import {
  type MutationFunctionContext,
  provideQueryClient,
} from "@tanstack/angular-query-experimental";
import { testQueryClient } from "../../../setupJest";
import { fromPartial, runMutationOnSuccess } from "../../test-helpers";
import { UtilService } from "../core/service/util.service";
import { PutBody } from "../shared/http/http-client";
import { GeneratedType } from "../shared/utils/generated-types";
import { OntkoppeldeDocumentenBulkFoutenDialogComponent } from "./ontkoppelde-documenten-bulk-fouten-dialog/ontkoppelde-documenten-bulk-fouten-dialog.component";
import { OntkoppeldeDocumentenService } from "./ontkoppelde-documenten.service";

describe(OntkoppeldeDocumentenService.name, () => {
  let service: OntkoppeldeDocumentenService;
  let utilService: UtilService;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideTranslateService(),
        provideHttpClient(withXhr()),
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

    it("marks every cached read of the detached documents stale, so that the werklijst re-reads instead of showing the deleted row", async () => {
      const listQueryKey = service.list(
        fromPartial<PutBody<"/rest/ontkoppeldedocumenten">>({ page: 0 }),
      ).queryKey;
      testQueryClient.setQueryData(listQueryKey, {});

      await runMutationOnSuccess(
        service.delete(),
        fromPartial<GeneratedType<"RestDetachedDocument">>({ id: 42 }),
      );

      expect(testQueryClient.getQueryState(listQueryKey)?.isInvalidated).toBe(
        true,
      );
    });
  });

  describe("deleteList", () => {
    it("sends the ids to the bulk delete endpoint", async () => {
      const request = service.deleteList().mutationFn!(
        { ids: [1, 2] },
        fromPartial<MutationFunctionContext>({}),
      );
      const testRequest = httpTestingController.expectOne(
        "/rest/ontkoppeldedocumenten/lijst/verwijderen",
      );
      testRequest.flush({ verwerkt: [1, 2], mislukt: [] });

      expect(testRequest.request.method).toBe("PUT");
      expect(testRequest.request.body).toEqual({ ids: [1, 2] });
      await request;
    });

    it("marks every cached read of the detached documents stale", async () => {
      const listQueryKey = service.list(
        fromPartial<PutBody<"/rest/ontkoppeldedocumenten">>({ page: 0 }),
      ).queryKey;
      testQueryClient.setQueryData(listQueryKey, {});

      await runMutationOnSuccess(service.deleteList());

      expect(testQueryClient.getQueryState(listQueryKey)?.isInvalidated).toBe(
        true,
      );
    });
  });

  describe("linkList", () => {
    it("sends the ids and the zaak to the bulk link endpoint", async () => {
      const request = service.linkList().mutationFn!(
        { ids: [1, 2], nieuweZaakID: "ZAAK-001" },
        fromPartial<MutationFunctionContext>({}),
      );
      const testRequest = httpTestingController.expectOne(
        "/rest/ontkoppeldedocumenten/lijst/koppelen",
      );
      testRequest.flush({ verwerkt: [1, 2], mislukt: [] });

      expect(testRequest.request.method).toBe("PUT");
      expect(testRequest.request.body).toEqual({
        ids: [1, 2],
        nieuweZaakID: "ZAAK-001",
      });
      await request;
    });

    it("marks every cached read of the detached documents stale", async () => {
      const listQueryKey = service.list(
        fromPartial<PutBody<"/rest/ontkoppeldedocumenten">>({ page: 0 }),
      ).queryKey;
      testQueryClient.setQueryData(listQueryKey, {});

      await runMutationOnSuccess(service.linkList());

      expect(testQueryClient.getQueryState(listQueryKey)?.isInvalidated).toBe(
        true,
      );
    });
  });

  describe("reportBulkResult", () => {
    it("uses the singular text when one document was processed", () => {
      service.reportBulkResult(
        { verwerkt: [1], mislukt: [] },
        "verwijderen",
        [],
      );

      expect(utilService.openSnackbar).toHaveBeenCalledWith(
        "msg.ontkoppelde-documenten.bulk.verwijderen.uitgevoerd.enkelvoud",
        { aantal: 1 },
      );
    });

    it("names a failed document without title after the selected document with the same id", () => {
      const open = jest
        .spyOn(TestBed.inject(MatDialog), "open")
        .mockReturnValue(fromPartial<MatDialogRef<unknown>>({}));

      service.reportBulkResult(
        {
          verwerkt: [],
          mislukt: [{ id: 5, foutcode: "NIET_GEVONDEN" }],
        },
        "verwijderen",
        [
          fromPartial<GeneratedType<"RestDetachedDocument">>({
            id: 5,
            titel: "Vijfde",
          }),
        ],
      );

      expect(open).toHaveBeenCalledWith(
        OntkoppeldeDocumentenBulkFoutenDialogComponent,
        {
          data: {
            actie: "verwijderen",
            mislukt: [{ id: 5, titel: "Vijfde", foutcode: "NIET_GEVONDEN" }],
          },
        },
      );
    });

    it("reports the number of processed documents in a snackbar", () => {
      service.reportBulkResult(
        { verwerkt: [1, 2, 3], mislukt: [] },
        "koppelen",
        [],
        {
          case: "ZAAK-001",
        },
      );

      expect(utilService.openSnackbar).toHaveBeenCalledWith(
        "msg.ontkoppelde-documenten.bulk.koppelen.uitgevoerd",
        { aantal: 3, case: "ZAAK-001" },
      );
    });

    it("opens no dialog when nothing failed", () => {
      const open = jest.spyOn(TestBed.inject(MatDialog), "open");

      service.reportBulkResult(
        { verwerkt: [1], mislukt: [] },
        "verwijderen",
        [],
      );

      expect(open).not.toHaveBeenCalled();
    });

    it("lists the failed documents in a dialog", () => {
      const open = jest
        .spyOn(TestBed.inject(MatDialog), "open")
        .mockReturnValue(fromPartial<MatDialogRef<unknown>>({}));
      const mislukt = [
        { id: 2, titel: "Tweede", foutcode: "VERGRENDELD" as const },
      ];

      service.reportBulkResult({ verwerkt: [1], mislukt }, "verwijderen", []);

      expect(open).toHaveBeenCalledWith(
        OntkoppeldeDocumentenBulkFoutenDialogComponent,
        { data: { actie: "verwijderen", mislukt } },
      );
    });
  });
});
