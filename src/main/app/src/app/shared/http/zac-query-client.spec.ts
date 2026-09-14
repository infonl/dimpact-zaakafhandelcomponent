/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  HttpEventType,
  provideHttpClient,
  withInterceptorsFromDi,
} from "@angular/common/http";
import {
  HttpTestingController,
  provideHttpClientTesting,
} from "@angular/common/http/testing";
import { TestBed } from "@angular/core/testing";
import { TranslateModule } from "@ngx-translate/core";
import type { MutationFunctionContext } from "@tanstack/angular-query-experimental";
import { EMPTY, of } from "rxjs";
import { fromPartial } from "../../../test-helpers";
import { UtilService } from "../../core/service/util.service";
import { FoutAfhandelingService } from "../../fout-afhandeling/fout-afhandeling.service";
import { ZacQueryClient } from "./zac-query-client";

describe(ZacQueryClient.name, () => {
  let zacQueryClient: ZacQueryClient;
  let httpTestingController: HttpTestingController;
  let foutAfhandelingService: FoutAfhandelingService;
  let utilService: UtilService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [TranslateModule.forRoot()],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
      ],
    });

    zacQueryClient = TestBed.inject(ZacQueryClient);
    httpTestingController = TestBed.inject(HttpTestingController);
    foutAfhandelingService = TestBed.inject(FoutAfhandelingService);
    utilService = TestBed.inject(UtilService);
  });

  describe("POST_WITH_PROGRESS", () => {
    const path =
      "/rest/informatieobjecten/informatieobject/{zaakUuid}/{documentReferenceId}" as const;
    const parameters = {
      path: { zaakUuid: "zaak-1", documentReferenceId: "reference-1" },
      query: { taakObject: false },
    };
    const url =
      "/rest/informatieobjecten/informatieobject/zaak-1/reference-1?taakObject=false";

    it("shows every percentage on the global progress indicator while uploading and resolves with the response body", async () => {
      const options = zacQueryClient.POST_WITH_PROGRESS(path, parameters);

      const response = options.mutationFn!(
        new FormData() as never,
        fromPartial<MutationFunctionContext>({}),
      );
      expect(utilService.progress()).toEqual({
        percentage: 0,
        description: "msg.document.uploaden.voortgang",
      });

      const request = httpTestingController.expectOne(url);
      request.event({
        type: HttpEventType.UploadProgress,
        loaded: 50,
        total: 100,
      });
      expect(utilService.progress()).toEqual({
        percentage: 50,
        description: "msg.document.uploaden.voortgang",
      });

      request.event({
        type: HttpEventType.UploadProgress,
        loaded: 100,
        total: 100,
      });
      request.flush({ uuid: "document-1" });

      expect(await response).toEqual({ uuid: "document-1" });
      expect(utilService.progress()).toEqual({
        percentage: 100,
        description: "msg.document.uploaden.voortgang",
      });
    });

    it("clears the global progress indicator once the upload has settled", async () => {
      const options = zacQueryClient.POST_WITH_PROGRESS(path, parameters);

      const response = options.mutationFn!(
        new FormData() as never,
        fromPartial<MutationFunctionContext>({}),
      );
      httpTestingController.expectOne(url).flush({ uuid: "document-1" });
      await response;
      options.onSettled!(
        { uuid: "document-1" } as never,
        null,
        new FormData() as never,
        undefined,
        fromPartial<MutationFunctionContext>({}),
      );

      expect(utilService.progress()).toBeNull();
    });

    it("reports the failure when the document is refused as too large", async () => {
      const foutAfhandelenSpy = jest
        .spyOn(foutAfhandelingService, "foutAfhandelen")
        .mockReturnValue(EMPTY);
      const options = zacQueryClient.POST_WITH_PROGRESS(path, parameters);

      const response = options.mutationFn!(
        new FormData() as never,
        fromPartial<MutationFunctionContext>({}),
      );
      httpTestingController
        .expectOne(url)
        .flush(
          { message: "msg.error.file.size-exceeded" },
          { status: 413, statusText: "Payload Too Large" },
        );

      await expect(response).rejects.toBeDefined();
      options.onError!(
        await response.catch((error) => error),
        new FormData() as never,
        undefined,
        fromPartial<MutationFunctionContext>({}),
      );
      expect(foutAfhandelenSpy).toHaveBeenCalled();
    });
  });

  describe("PUT_WITH_PROGRESS", () => {
    it("shows every percentage on the global progress indicator while uploading a new version and resolves with the response body", async () => {
      const options = zacQueryClient.PUT_WITH_PROGRESS(
        "/rest/informatieobjecten/informatieobject/{uuid}",
        { path: { uuid: "document-1" }, query: { zaak: "zaak-1" } },
      );

      const response = options.mutationFn!(
        new FormData() as never,
        fromPartial<MutationFunctionContext>({}),
      );
      const request = httpTestingController.expectOne(
        "/rest/informatieobjecten/informatieobject/document-1?zaak=zaak-1",
      );
      expect(request.request.method).toBe("PUT");
      request.event({
        type: HttpEventType.UploadProgress,
        loaded: 50,
        total: 100,
      });
      expect(utilService.progress()).toEqual({
        percentage: 50,
        description: "msg.document.uploaden.voortgang",
      });

      request.flush({ uuid: "document-1", versie: 2 });

      expect(await response).toEqual({ uuid: "document-1", versie: 2 });
    });
  });

  describe("DELETE", () => {
    describe("an endpoint addressed by a path parameter", () => {
      it("takes the parameter from the variables it is mutated with", async () => {
        const options = zacQueryClient.DELETE(
          "/rest/notities/{id}",
          (id: number) => ({ parameters: { path: { id } } }),
        );

        const response = options.mutationFn!(
          42,
          fromPartial<MutationFunctionContext>({}),
        );
        httpTestingController.expectOne("/rest/notities/42").flush(null);

        await response;
      });

      it("sends the body the request derives from those same variables", async () => {
        const options = zacQueryClient.DELETE(
          "/rest/zaken/{uuid}/initiator",
          ({ zaakUuid, reden }: { zaakUuid: string; reden: string }) => ({
            parameters: { path: { uuid: zaakUuid } },
            body: { reden },
          }),
        );

        const response = options.mutationFn!(
          { zaakUuid: "fakeZaakUuid", reden: "fakeReden" },
          fromPartial<MutationFunctionContext>({}),
        );
        const request = httpTestingController.expectOne(
          "/rest/zaken/fakeZaakUuid/initiator",
        );
        request.flush(null);

        expect(request.request.body).toEqual({ reden: "fakeReden" });
        await response;
      });
    });

    describe("an endpoint without path parameters", () => {
      it("sends the variables as the request body", async () => {
        const options = zacQueryClient.DELETE("/rest/bag");

        const response = options.mutationFn!(
          { zaakUuid: "fakeZaakUuid" },
          fromPartial<MutationFunctionContext>({}),
        );
        const request = httpTestingController.expectOne("/rest/bag");
        request.flush(null);

        expect(request.request.body).toEqual({ zaakUuid: "fakeZaakUuid" });
        await response;
      });
    });

    it("reports a failure through the error handling", () => {
      const foutAfhandelen = jest
        .spyOn(foutAfhandelingService, "foutAfhandelen")
        .mockReturnValue(of());
      const options = zacQueryClient.DELETE(
        "/rest/notities/{id}",
        (id: number) => ({
          parameters: { path: { id } },
        }),
      );

      options.onError!(
        { status: 500 } as never,
        42 as never,
        undefined,
        fromPartial<MutationFunctionContext>({}),
      );

      expect(foutAfhandelen).toHaveBeenCalled();
    });
  });

  describe("PUT_QUERY", () => {
    it("keys the query on the endpoint, the body and the path parameters", () => {
      const query = zacQueryClient.PUT_QUERY(
        "/rest/signaleringen/zaken/{type}",
        { page: 0, rows: 5, sortField: "CREATED", sortOrder: "DESC" },
        { path: { type: "ZAAK_OP_NAAM" } },
      );

      expect(query.queryKey).toEqual([
        "/rest/signaleringen/zaken/{type}",
        { page: 0, rows: 5, sortField: "CREATED", sortOrder: "DESC" },
        { path: { type: "ZAAK_OP_NAAM" } },
      ]);
    });

    it("gives two sets of filters two cache entries", () => {
      const first = zacQueryClient.PUT_QUERY("/rest/zoeken/list", {
        page: 0,
        rows: 10,
      });
      const second = zacQueryClient.PUT_QUERY("/rest/zoeken/list", {
        page: 1,
        rows: 10,
      });

      expect(first.queryKey).not.toEqual(second.queryKey);
    });

    it("sends the body as a PUT and resolves with the response", async () => {
      const query = zacQueryClient.PUT_QUERY("/rest/zoeken/list", {
        page: 0,
        rows: 10,
      });

      const response = query.queryFn!({} as never);
      const request = httpTestingController.expectOne({
        method: "PUT",
        url: "/rest/zoeken/list",
      });
      expect(request.request.body).toEqual({ page: 0, rows: 10 });
      request.flush({ totaal: 0 });

      await expect(response).resolves.toEqual({ totaal: 0 });
    });
  });
});
