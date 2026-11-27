/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  HttpErrorResponse,
  HttpEventType,
  provideHttpClient,
  withInterceptorsFromDi,
  withXhr,
} from "@angular/common/http";
import {
  HttpTestingController,
  provideHttpClientTesting,
  type TestRequest,
} from "@angular/common/http/testing";
import { TestBed } from "@angular/core/testing";
import { provideTranslateService } from "@ngx-translate/core";
import type { MutationFunctionContext } from "@tanstack/angular-query-experimental";
import { EMPTY, lastValueFrom, of } from "rxjs";
import { sleep, testQueryClient } from "../../../../setupJest";
import { fromPartial } from "../../../test-helpers";
import { UtilService } from "../../core/service/util.service";
import { FoutAfhandelingService } from "../../fout-afhandeling/fout-afhandeling.service";
import { search } from "./invalidation";
import { INVALIDATION_MAP } from "./invalidation-map";
import { mergeMutationOptions } from "./merge-mutation-options";
import { runMutation } from "./run-mutation";
import { ZacQueryClient } from "./zac-query-client";

type Mutable<Type> = { -readonly [Key in keyof Type]: Type[Key] };

describe(ZacQueryClient.name, () => {
  let zacQueryClient: ZacQueryClient;
  let httpTestingController: HttpTestingController;
  let foutAfhandelingService: FoutAfhandelingService;
  let utilService: UtilService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideTranslateService(),
        provideHttpClient(withXhr(), withInterceptorsFromDi()),
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

    it("leaves the error dialog closed for a mutation that reports its own failure", () => {
      const foutAfhandelenSpy = jest
        .spyOn(foutAfhandelingService, "foutAfhandelen")
        .mockReturnValue(EMPTY);
      const options = mergeMutationOptions(
        zacQueryClient.POST_WITH_PROGRESS(path, parameters),
        { meta: { reportErrors: false } },
      );

      options.onError!(
        new HttpErrorResponse({ status: 500 }),
        new FormData() as never,
        undefined,
        fromPartial<MutationFunctionContext>({ meta: options.meta }),
      );

      expect(foutAfhandelenSpy).not.toHaveBeenCalled();
    });
    describe("with the invalidation map", () => {
      const documentList = [
        "/rest/informatieobjecten/informatieobjectenList",
        { zaakUUID: "zaak-1", shouldIncludeGekoppeldeZaakDocumenten: false },
      ];
      const isInvalidated = () =>
        testQueryClient.getQueryState(documentList)?.isInvalidated;

      async function upload(answer: (request: TestRequest) => void) {
        testQueryClient.setQueryData(documentList, "fakeData");
        const uploaded = lastValueFrom(
          runMutation(
            testQueryClient,
            zacQueryClient.POST_WITH_PROGRESS(path, parameters),
            new FormData() as never,
          ),
        ).catch(() => undefined);
        await sleep();
        answer(httpTestingController.expectOne(url));
        await uploaded;
      }

      afterEach(() => {
        jest.restoreAllMocks();
      });

      it("refreshes the documents of the zaak after an upload that succeeds", async () => {
        await upload((request) => request.flush({ uuid: "document-1" }));

        expect(isInvalidated()).toBe(true);
      });

      it("applies a rule that sets afterFailureToo after an upload that fails, and still clears the global progress indicator", async () => {
        jest.replaceProperty(
          INVALIDATION_MAP as Mutable<typeof INVALIDATION_MAP>,
          `POST ${path}`,
          {
            afterFailureToo: true,
            refresh: () => [
              search("/rest/informatieobjecten/informatieobjectenList"),
            ],
          },
        );
        jest
          .spyOn(foutAfhandelingService, "foutAfhandelen")
          .mockReturnValue(EMPTY);

        await upload((request) =>
          request.flush(null, { status: 400, statusText: "Bad Request" }),
        );

        expect(isInvalidated()).toBe(true);
        expect(utilService.progress()).toBeNull();
      });
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

    it("leaves the error dialog closed for a mutation that reports its own failure", () => {
      const foutAfhandelenSpy = jest
        .spyOn(foutAfhandelingService, "foutAfhandelen")
        .mockReturnValue(EMPTY);
      const options = mergeMutationOptions(
        zacQueryClient.PUT_WITH_PROGRESS(
          "/rest/informatieobjecten/informatieobject/{uuid}",
          { path: { uuid: "document-1" }, query: { zaak: "zaak-1" } },
        ),
        { meta: { reportErrors: false } },
      );

      options.onError!(
        new HttpErrorResponse({ status: 500 }),
        new FormData() as never,
        undefined,
        fromPartial<MutationFunctionContext>({ meta: options.meta }),
      );

      expect(foutAfhandelenSpy).not.toHaveBeenCalled();
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

  describe("the invalidation map", () => {
    const list = ["/rest/zoeken/list", { page: 0 }];
    const path = "/rest/taken/lijst/vrijgeven";
    const isInvalidated = () =>
      testQueryClient.getQueryState(list)?.isInvalidated;

    async function release(answer: (request: TestRequest) => void) {
      testQueryClient.setQueryData(list, "fakeData");
      const staleInOnSuccess: unknown[] = [];
      const released = lastValueFrom(
        runMutation(
          testQueryClient,
          mergeMutationOptions(zacQueryClient.PUT(path), {
            onSuccess: () => void staleInOnSuccess.push(isInvalidated()),
          }),
          fromPartial({}),
        ),
      ).catch(() => undefined);
      await sleep();
      answer(httpTestingController.expectOne(path));
      await released;
      return staleInOnSuccess;
    }

    it("is applied after a write that succeeds, once the callbacks of the callers have run", async () => {
      const staleInOnSuccess = await release((request) => request.flush(null));

      expect(staleInOnSuccess).toEqual([false]);
      expect(isInvalidated()).toBe(true);
    });

    it("is not applied after a write that fails", async () => {
      jest
        .spyOn(foutAfhandelingService, "foutAfhandelen")
        .mockReturnValue(EMPTY);

      await release((request) =>
        request.flush(null, { status: 400, statusText: "Bad Request" }),
      );

      expect(isInvalidated()).toBe(false);
    });

    describe("with a rule that sets afterFailureToo", () => {
      function ruleOf(rule: { awaitRefresh?: true }) {
        jest.replaceProperty(
          INVALIDATION_MAP as Mutable<typeof INVALIDATION_MAP>,
          `PUT ${path}`,
          {
            afterFailureToo: true,
            refresh: () => [search("/rest/zoeken/list")],
            ...rule,
          },
        );
      }

      afterEach(() => {
        jest.restoreAllMocks();
      });

      it("is applied after a write that succeeds", async () => {
        ruleOf({});

        await release((request) => request.flush(null));

        expect(isInvalidated()).toBe(true);
      });

      it("is applied after a write that fails", async () => {
        ruleOf({});
        jest
          .spyOn(foutAfhandelingService, "foutAfhandelen")
          .mockReturnValue(EMPTY);

        await release((request) =>
          request.flush(null, { status: 400, statusText: "Bad Request" }),
        );

        expect(isInvalidated()).toBe(true);
      });

      it.each([
        ["keeps the write pending until the refresh is done", true, false],
        ["does not wait for the refresh", undefined, true],
      ] as const)(
        "%s when awaitRefresh is %s",
        async (_description, awaitRefresh, isDoneBeforeRefresh) => {
          ruleOf({ awaitRefresh });
          let finishRefresh!: () => void;
          jest
            .spyOn(testQueryClient, "invalidateQueries")
            .mockReturnValue(
              new Promise((resolve) => (finishRefresh = resolve)),
            );
          let isDone = false;

          const released = release((request) => request.flush(null)).then(
            () => (isDone = true),
          );
          await sleep();
          await sleep();
          expect(isDone).toBe(isDoneBeforeRefresh);

          finishRefresh();
          await released;
          expect(isDone).toBe(true);
        },
      );
    });
  });
});
