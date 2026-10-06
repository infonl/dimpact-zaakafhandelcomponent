/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  provideHttpClient,
  withInterceptorsFromDi,
} from "@angular/common/http";
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
import { fromPartial, runMutationOnSuccess } from "src/test-helpers";
import { testQueryClient } from "../../../setupJest";
import { UtilService } from "../core/service/util.service";
import { GeneratedType } from "../shared/utils/generated-types";
import { ZakenService } from "./zaken.service";

describe(ZakenService.name, () => {
  let service: ZakenService;
  let utilService: UtilService;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [TranslateModule.forRoot()],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
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

  describe.each([
    ["updateMutation", () => service.updateMutation()],
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
});
