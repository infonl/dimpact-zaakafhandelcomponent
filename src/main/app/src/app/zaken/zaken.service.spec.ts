/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  provideHttpClient,
  withInterceptorsFromDi,
} from "@angular/common/http";
import { TestBed } from "@angular/core/testing";
import { TranslateModule } from "@ngx-translate/core";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { fromPartial, runMutationOnSuccess } from "src/test-helpers";
import { testQueryClient } from "../../../setupJest";
import { UtilService } from "../core/service/util.service";
import { GeneratedType } from "../shared/utils/generated-types";
import { ZakenService } from "./zaken.service";

describe(ZakenService.name, () => {
  let service: ZakenService;
  let utilService: UtilService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [TranslateModule.forRoot()],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideQueryClient(testQueryClient),
      ],
    });

    service = TestBed.inject(ZakenService);
    utilService = TestBed.inject(UtilService);
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
});
