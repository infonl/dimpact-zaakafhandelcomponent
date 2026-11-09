/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  HttpErrorResponse,
  provideHttpClient,
  withInterceptorsFromDi,
  withXhr,
} from "@angular/common/http";
import {
  HttpTestingController,
  provideHttpClientTesting,
} from "@angular/common/http/testing";
import { TestBed } from "@angular/core/testing";
import { provideTranslateService } from "@ngx-translate/core";
import {
  type MutationFunctionContext,
  provideQueryClient,
} from "@tanstack/angular-query-experimental";
import { EMPTY } from "rxjs";
import { fromPartial, runMutationOnSuccess } from "src/test-helpers";
import { sleep, testQueryClient } from "../../../setupJest";
import { UtilService } from "../core/service/util.service";
import { FoutAfhandelingService } from "../fout-afhandeling/fout-afhandeling.service";
import { QUERY_CLIENT } from "../shared/http/query-client";
import { GeneratedType } from "../shared/utils/generated-types";
import { ZakenService } from "./zaken.service";

describe(ZakenService.name, () => {
  let service: ZakenService;
  let utilService: UtilService;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideTranslateService(),
        provideHttpClient(withXhr(), withInterceptorsFromDi()),
        provideHttpClientTesting(),
        provideQueryClient(testQueryClient),
      ],
    });

    service = TestBed.inject(ZakenService);
    utilService = TestBed.inject(UtilService);
    httpTestingController = TestBed.inject(HttpTestingController);
    jest.spyOn(utilService, "openSnackbar").mockImplementation(() => {});
  });

  describe("toekennenAanIngelogdeMedewerkerVanuitLijst", () => {
    it("names the behandelaar the zaak was assigned to", async () => {
      await runMutationOnSuccess(
        service.toekennenAanIngelogdeMedewerkerVanuitLijst(),
        undefined,
        fromPartial<GeneratedType<"RestZaakOverzicht">>({
          behandelaar: { naam: "fakeBehandelaarNaam" },
        }),
      );

      expect(utilService.openSnackbar).toHaveBeenCalledWith(
        "msg.zaak.toegekend",
        { behandelaar: "fakeBehandelaarNaam" },
      );
    });

    it("still confirms the assignment when the response names no behandelaar", async () => {
      await runMutationOnSuccess(
        service.toekennenAanIngelogdeMedewerkerVanuitLijst(),
        undefined,
        fromPartial<GeneratedType<"RestZaakOverzicht">>({}),
      );

      expect(utilService.openSnackbar).toHaveBeenCalledWith(
        "msg.zaak.toegekend",
        { behandelaar: undefined },
      );
    });
  });

  describe("ontkoppelInformatieObject", () => {
    it("names the document it unlinked", async () => {
      await runMutationOnSuccess(
        service.ontkoppelInformatieObject(
          fromPartial<GeneratedType<"RestEnkelvoudigInformatieobject">>({
            titel: "fakeDocumentTitel",
          }),
        ),
      );

      expect(utilService.openSnackbar).toHaveBeenCalledWith(
        "msg.document.ontkoppelen.uitgevoerd",
        { document: "fakeDocumentTitel" },
      );
    });
  });

  describe("readZaakQuery", () => {
    it("keys on the uuid endpoint so every caller shares one cache entry", () => {
      expect(service.readZaakQuery("fakeZaakUuid1").queryKey).toEqual([
        "/rest/zaken/zaak/{uuid}",
        { path: { uuid: "fakeZaakUuid1" } },
      ]);
    });
  });

  describe("readZaak", () => {
    const cachedZaak = fromPartial<GeneratedType<"RestZaak">>({
      uuid: "fakeZaakUuid1",
      omschrijving: "fakeCachedOmschrijving",
    });
    const serverZaak = fromPartial<GeneratedType<"RestZaak">>({
      uuid: "fakeZaakUuid1",
      omschrijving: "fakeServerOmschrijving",
    });

    it("asks the server even when the zaak is cached, because the taak view reads it after a change", async () => {
      service.cacheZaak(cachedZaak);

      const read = testQueryClient.query(service.readZaak("fakeZaakUuid1"));
      await sleep();
      httpTestingController
        .expectOne("/rest/zaken/zaak/fakeZaakUuid1")
        .flush(serverZaak);

      await expect(read).resolves.toEqual(serverZaak);
    });

    it("writes the answer into the cache entry that the zaak view reads", async () => {
      const read = testQueryClient.query(service.readZaak("fakeZaakUuid1"));
      await sleep();
      httpTestingController
        .expectOne("/rest/zaken/zaak/fakeZaakUuid1")
        .flush(serverZaak);
      await read;

      expect(
        testQueryClient.getQueryData(
          service.readZaakQuery("fakeZaakUuid1").queryKey,
        ),
      ).toEqual(serverZaak);
    });

    it("opens the error dialog when the read fails over a cached zaak, because the user waits on it", () => {
      expect(service.readZaak("fakeZaakUuid1").meta).toEqual({
        reportFailedRefetch: true,
      });
    });
  });

  describe("readZaakByID", () => {
    const zaak = fromPartial<GeneratedType<"RestZaak">>({
      uuid: "fakeZaakUuid1",
      identificatie: "ZAAK-2026-0000000001",
    });

    async function readOnce() {
      const read = testQueryClient.query(
        service.readZaakByID("ZAAK-2026-0000000001"),
      );
      await sleep();
      httpTestingController
        .expectOne("/rest/zaken/zaak/id/ZAAK-2026-0000000001")
        .flush(zaak);
      return read;
    }

    it("asks the server every time, because a route or a document opens it once per visit", async () => {
      await readOnce();
      await sleep();

      await expect(readOnce()).resolves.toEqual(zaak);
    });

    it("keeps no copy of the zaak after the read", async () => {
      await readOnce();
      await sleep();

      expect(
        testQueryClient.getQueryState(
          service.readZaakByID("ZAAK-2026-0000000001").queryKey,
        ),
      ).toBeUndefined();
    });
  });

  describe("cacheZaak", () => {
    it("writes the zaak into the cache entry for its own uuid", () => {
      const zaak = fromPartial<GeneratedType<"RestZaak">>({
        uuid: "fakeZaakUuid1",
        omschrijving: "fakeOmschrijving1",
      });

      service.cacheZaak(zaak);

      expect(
        testQueryClient.getQueryData(
          service.readZaakQuery("fakeZaakUuid1").queryKey,
        ),
      ).toBe(zaak);
    });
  });

  describe("invalidateHistorie", () => {
    it("invalidates the historie of the zaak it is given", () => {
      const invalidateQueries = jest.spyOn(
        testQueryClient,
        "invalidateQueries",
      );

      service.invalidateHistorie("fakeZaakUuid1");

      expect(invalidateQueries).toHaveBeenCalledWith(
        {
          queryKey: service.listHistorieVoorZaakQuery("fakeZaakUuid1").queryKey,
        },
        expect.anything(),
      );
    });

    it("leaves a refetch that is already running alone, because the zaak view invalidates repeatedly", () => {
      const invalidateQueries = jest.spyOn(
        testQueryClient,
        "invalidateQueries",
      );

      service.invalidateHistorie("fakeZaakUuid1");

      expect(invalidateQueries).toHaveBeenCalledWith(expect.anything(), {
        cancelRefetch: false,
      });
    });
  });

  describe.each([
    [
      "updateBesluit",
      () =>
        runMutationOnSuccess(
          service.updateBesluit(),
          fromPartial<GeneratedType<"RestBesluitChangeData">>({
            besluitUuid: "fakeBesluitUuid",
          }),
        ),
    ],
    [
      "intrekkenBesluit",
      () =>
        runMutationOnSuccess(
          service.intrekkenBesluit(),
          fromPartial<GeneratedType<"RestBesluitWithdrawalData">>({
            besluitUuid: "fakeBesluitUuid",
          }),
        ),
    ],
  ])("%s", (_name, runOnSuccess) => {
    it("refreshes the historie of the besluit it changed, because the change adds a line to it", async () => {
      const invalidateQueries = jest.spyOn(
        testQueryClient,
        "invalidateQueries",
      );

      await runOnSuccess();

      expect(invalidateQueries).toHaveBeenCalledWith(
        {
          queryKey: service.listBesluitHistorie("fakeBesluitUuid").queryKey,
        },
        { cancelRefetch: false },
      );
    });
  });

  describe("listBesluitHistorie", () => {
    it("opens the error dialog when a refetch fails, because the user waits on it after changing the besluit", () => {
      expect(service.listBesluitHistorie("fakeBesluitUuid").meta).toEqual({
        reportFailedRefetch: true,
      });
    });
  });

  describe("deleteInitiator", () => {
    it("addresses the zaak by its uuid and sends the reden as the body", async () => {
      const request = service.deleteInitiator().mutationFn!(
        { zaakUuid: "fakeZaakUuid", reden: "fakeReden" },
        fromPartial<MutationFunctionContext>({}),
      );
      const httpRequest = httpTestingController.expectOne(
        "/rest/zaken/fakeZaakUuid/initiator",
      );
      httpRequest.flush(null);

      expect(httpRequest.request.body).toEqual({ reden: "fakeReden" });
      await request;
    });
  });

  describe("updateZaak", () => {
    it("addresses the zaak by its uuid and sends only the changed zaakgegevens with the reden", async () => {
      const request = service.updateZaak("fakeZaakUuid").mutationFn!(
        { zaak: { omschrijving: "fakeOmschrijving" }, reden: "fakeReden" },
        fromPartial<MutationFunctionContext>({}),
      );
      const httpRequest = httpTestingController.expectOne(
        "/rest/zaken/zaak/fakeZaakUuid",
      );
      httpRequest.flush({});

      expect(httpRequest.request.method).toBe("PATCH");
      expect(httpRequest.request.body).toEqual({
        zaak: { omschrijving: "fakeOmschrijving" },
        reden: "fakeReden",
      });
      await request;
    });

    it("sends the release of the behandelaar along with the zaakgegevens", async () => {
      const request = service.updateZaak("fakeZaakUuid").mutationFn!(
        { zaak: {}, reden: "fakeReden", isBehandelaarVrijgegeven: true },
        fromPartial<MutationFunctionContext>({}),
      );
      const httpRequest = httpTestingController.expectOne(
        "/rest/zaken/zaak/fakeZaakUuid",
      );
      httpRequest.flush({});

      expect(httpRequest.request.body).toEqual({
        zaak: {},
        reden: "fakeReden",
        isBehandelaarVrijgegeven: true,
      });
      await request;
    });
  });

  describe.each([
    [
      "suspendZaak",
      () => service.suspendZaak("fakeZaakUuid"),
      "msg.zaak.opgeschort",
    ],
    ["resumeZaak", () => service.resumeZaak("fakeZaakUuid"), "msg.zaak.hervat"],
    ["afbreken", () => service.afbreken("fakeZaakUuid"), "msg.zaak.afgebroken"],
    ["heropenen", () => service.heropenen("fakeZaakUuid"), "msg.zaak.heropend"],
  ])("%s", (_name, createMutation, message) => {
    it(`confirms the change with ${message}`, async () => {
      await createMutation().onSuccess?.(
        fromPartial<GeneratedType<"RestZaak">>({ uuid: "fakeZaakUuid" }),
        fromPartial({}),
        undefined,
        fromPartial<MutationFunctionContext>({}),
      );

      expect(utilService.openSnackbar).toHaveBeenCalledWith(message);
    });
  });

  describe("ontkoppelZaak", () => {
    const unlinkData: GeneratedType<"RestZaakUnlinkData"> = {
      zaakUuid: "fakeZaakUuid",
      gekoppeldeZaakIdentificatie: "ZAAK-002",
      relatieType: "GERELATEERD",
      reden: "fakeReden",
    };

    it("refetches the zaak it unlinked from, because the server answers without it", async () => {
      const invalidateZaak = jest.spyOn(service, "invalidateZaak");

      await runMutationOnSuccess(service.ontkoppelZaak(), unlinkData);

      expect(invalidateZaak).toHaveBeenCalledWith("fakeZaakUuid");
    });

    it("confirms the unlinking", async () => {
      await runMutationOnSuccess(service.ontkoppelZaak(), unlinkData);

      expect(utilService.openSnackbar).toHaveBeenCalledWith(
        "msg.zaak.ontkoppelen.uitgevoerd",
      );
    });
  });

  describe("deleteBetrokkene", () => {
    it("addresses the rol by its uuid and sends the reden as the body", async () => {
      const request = service.deleteBetrokkene().mutationFn!(
        { rolUuid: "fakeRolUuid", reden: "fakeReden" },
        fromPartial<MutationFunctionContext>({}),
      );
      const httpRequest = httpTestingController.expectOne(
        "/rest/zaken/betrokkene/fakeRolUuid",
      );
      httpRequest.flush(null);

      expect(httpRequest.request.body).toEqual({ reden: "fakeReden" });
      await request;
    });
  });

  describe("deleteZaakspecifiekGeautoriseerdeMedewerker", () => {
    const variables = { medewerkerId: "fakeMedewerkerId", reden: "fakeReden" };

    it("addresses the medewerker of the zaak and sends the reden as the body", async () => {
      const request = service.deleteZaakspecifiekGeautoriseerdeMedewerker(
        "fakeZaakUuid",
      ).mutationFn!(variables, fromPartial<MutationFunctionContext>({}));
      const httpRequest = httpTestingController.expectOne(
        "/rest/zaken/zaak/fakeZaakUuid/zaakspecifiek-geautoriseerde-medewerkers/fakeMedewerkerId",
      );
      httpRequest.flush(null);

      expect(httpRequest.request.method).toBe("DELETE");
      expect(httpRequest.request.body).toEqual({ reden: "fakeReden" });
      await request;
    });

    it("refreshes the medewerkers, the kandidaten and the historie of the zaak, because they all changed", async () => {
      const invalidateQueries = jest.spyOn(
        testQueryClient,
        "invalidateQueries",
      );
      const invalidateHistorie = jest.spyOn(service, "invalidateHistorie");

      await runMutationOnSuccess(
        service.deleteZaakspecifiekGeautoriseerdeMedewerker("fakeZaakUuid"),
        variables,
      );

      expect(invalidateQueries).toHaveBeenCalledWith({
        queryKey:
          service.listGeautoriseerdeMedewerkersQuery("fakeZaakUuid").queryKey,
      });
      expect(invalidateQueries).toHaveBeenCalledWith({
        queryKey: [
          "/rest/zaken/zaak/{uuid}/zaakspecifiek-geautoriseerde-medewerkers/kandidaten",
        ],
      });
      expect(invalidateHistorie).toHaveBeenCalledWith("fakeZaakUuid");
    });

    it("refreshes nothing after the logged-in user detaches themselves, because they may no longer read the zaak", async () => {
      const invalidateQueries = jest.spyOn(
        testQueryClient,
        "invalidateQueries",
      );

      await runMutationOnSuccess(
        service.deleteZaakspecifiekGeautoriseerdeMedewerker("fakeZaakUuid"),
        { ...variables, isLoggedInUser: true },
      );

      expect(invalidateQueries).not.toHaveBeenCalled();
    });
  });

  describe.each([
    ["updateZaak", () => service.updateZaak("fakeZaakUuid1")],
    ["suspendZaak", () => service.suspendZaak("fakeZaakUuid1")],
    ["resumeZaak", () => service.resumeZaak("fakeZaakUuid1")],
    ["afbreken", () => service.afbreken("fakeZaakUuid1")],
    ["heropenen", () => service.heropenen("fakeZaakUuid1")],
    ["verlengenZaak", () => service.verlengenZaak("fakeZaakUuid1")],
    ["updateZaakLocatie", () => service.updateZaakLocatie("fakeZaakUuid1")],
    ["afsluitenMutation", () => service.afsluitenMutation("fakeZaakUuid1")],
    ["updateInitiator", () => service.updateInitiator()],
    ["createBetrokkene", () => service.createBetrokkene()],
    ["deleteInitiator", () => service.deleteInitiator()],
    ["deleteBetrokkene", () => service.deleteBetrokkene()],
  ])("%s", (_name, createMutation) => {
    const zaak = fromPartial<GeneratedType<"RestZaak">>({
      uuid: "fakeZaakUuid1",
    });

    it("caches the zaak the server answers with", () => {
      const cacheZaak = jest.spyOn(service, "cacheZaak");

      createMutation().onSuccess?.(
        zaak,
        fromPartial({}),
        undefined,
        fromPartial<MutationFunctionContext>({}),
      );

      expect(cacheZaak).toHaveBeenCalledWith(zaak);
    });

    it("keeps the zaak the server answers with when a refetch that started before the save resolves after it", async () => {
      const queryKey = service.readZaakQuery("fakeZaakUuid1").queryKey;
      let resolveRefetch!: (zaak: GeneratedType<"RestZaak">) => void;
      const refetch = testQueryClient
        .fetchQuery({
          queryKey,
          queryFn: () =>
            new Promise<GeneratedType<"RestZaak">>((resolve) => {
              resolveRefetch = resolve;
            }),
        })
        .catch(() => undefined);

      createMutation().onSuccess?.(
        zaak,
        fromPartial({}),
        undefined,
        fromPartial<MutationFunctionContext>({}),
      );
      resolveRefetch(
        fromPartial<GeneratedType<"RestZaak">>({
          uuid: "fakeZaakUuid1",
          omschrijving: "fakeZaakBeforeTheSave",
        }),
      );
      await refetch;

      expect(testQueryClient.getQueryData(queryKey)).toBe(zaak);
    });

    it("refetches the zaak after caching it, so a concurrent change that a cancelled refetch carried still shows", () => {
      const cacheZaak = jest.spyOn(service, "cacheZaak");
      const invalidateZaak = jest.spyOn(service, "invalidateZaak");

      createMutation().onSuccess?.(
        zaak,
        fromPartial({}),
        undefined,
        fromPartial<MutationFunctionContext>({}),
      );

      expect(invalidateZaak).toHaveBeenCalledWith("fakeZaakUuid1");
      expect(cacheZaak.mock.invocationCallOrder[0]).toBeLessThan(
        invalidateZaak.mock.invocationCallOrder[0],
      );
    });

    it("refreshes the historie of that zaak, because the server accepted the change", () => {
      const invalidateHistorie = jest.spyOn(service, "invalidateHistorie");

      createMutation().onSuccess?.(
        zaak,
        fromPartial({}),
        undefined,
        fromPartial<MutationFunctionContext>({}),
      );

      expect(invalidateHistorie).toHaveBeenCalledWith("fakeZaakUuid1");
    });
  });

  describe("the afzenders of a zaak", () => {
    const afzendersUrl = "/rest/zaken/zaak/fakeZaakUuid/afzender";
    const afzender = fromPartial<GeneratedType<"RestZaakAfzender">>({
      mail: "fakeAfzender@example.com",
      suffix: "gegevens.mail.afzender.MEDEWERKER",
    });

    async function flushAfterRequest(url: string, body: unknown) {
      await Promise.resolve();
      httpTestingController.expectOne(url).flush(body as never);
    }

    it("gives the suffix as a translation key to a one-shot read as well, so that a task form shows the same label as a component", async () => {
      const read = testQueryClient.query(
        service.listAfzendersVoorZaak("fakeZaakUuid"),
      );
      await flushAfterRequest(afzendersUrl, [afzender]);

      expect((await read)[0].suffix).toBe("gegevens.mail.afzender.medewerker");
    });

    it("does not change a cached afzender in place, so that the suffix conversion cannot corrupt the cache", async () => {
      const options = service.listAfzendersVoorZaak("fakeZaakUuid");
      const cached = Object.freeze([Object.freeze({ ...afzender })]);
      testQueryClient.setQueryData(options.queryKey, cached as never);

      await expect(testQueryClient.query(options)).resolves.toEqual([
        { ...afzender, suffix: "gegevens.mail.afzender.medewerker" },
      ]);
    });

    it("reads from the cache the second time, without a request", async () => {
      const options = service.listAfzendersVoorZaak("fakeZaakUuid");
      const first = testQueryClient.query(options);
      await flushAfterRequest(afzendersUrl, [afzender]);
      await first;

      await testQueryClient.query(options);

      httpTestingController.expectNone(afzendersUrl);
    });

    it("reads the afzenders of every zaak again after invalidateAfzenders, so that a changed zaaktype configuration reaches the forms", async () => {
      const afzenderOptions = service.listAfzendersVoorZaak("fakeZaakUuid");
      const otherAfzenderOptions =
        service.listAfzendersVoorZaak("fakeOtherZaakUuid");
      testQueryClient.setQueryData(afzenderOptions.queryKey, [afzender]);
      testQueryClient.setQueryData(otherAfzenderOptions.queryKey, [afzender]);

      await service.invalidateAfzenders();

      expect(
        testQueryClient.getQueryState(afzenderOptions.queryKey)?.isInvalidated,
      ).toBe(true);
      expect(
        testQueryClient.getQueryState(otherAfzenderOptions.queryKey)
          ?.isInvalidated,
      ).toBe(true);
    });

    it("leaves the other reads of a zaak alone on invalidateAfzenders", async () => {
      const zaakOptions = service.readZaakQuery("fakeZaakUuid");
      testQueryClient.setQueryData(
        zaakOptions.queryKey,
        fromPartial<GeneratedType<"RestZaak">>({ uuid: "fakeZaakUuid" }),
      );

      await service.invalidateAfzenders();

      expect(
        testQueryClient.getQueryState(zaakOptions.queryKey)?.isInvalidated,
      ).toBe(false);
    });

    it("opens the error dialog on a failed re-read, as opening a form is an action the user waits on", async () => {
      const foutAfhandelen = jest
        .spyOn(TestBed.inject(FoutAfhandelingService), "foutAfhandelen")
        .mockReturnValue(EMPTY);
      const queryClient = TestBed.inject(QUERY_CLIENT);
      const options = service.listAfzendersVoorZaak("fakeZaakUuid");
      queryClient.setQueryData(options.queryKey, [afzender]);

      const reRead = queryClient.fetchQuery({
        ...options,
        retry: false,
        staleTime: 0,
      });
      await Promise.resolve();
      httpTestingController
        .expectOne(afzendersUrl)
        .flush(null, { status: 500, statusText: "Internal Server Error" });
      await expect(reRead).rejects.toBeInstanceOf(HttpErrorResponse);

      expect(foutAfhandelen).toHaveBeenCalledWith(
        expect.objectContaining({ status: 500 }),
      );
    });
  });
});
