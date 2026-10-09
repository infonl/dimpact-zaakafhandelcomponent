/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  provideHttpClient,
  withInterceptorsFromDi,
} from "@angular/common/http";
import { TestBed } from "@angular/core/testing";
import { Router } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { of } from "rxjs";
import { fromPartial, runMutationOnSuccess } from "src/test-helpers";
import { testQueryClient } from "../../../setupJest";
import { UtilService } from "../core/service/util.service";
import { ZacHttpClient } from "../shared/http/zac-http-client";
import { ZacQueryClient } from "../shared/http/zac-query-client";
import { GeneratedType } from "../shared/utils/generated-types";
import { TakenService } from "./taken.service";

describe(TakenService.name, () => {
  let service: TakenService;
  let zacHttpClient: ZacHttpClient;
  let zacQueryClient: ZacQueryClient;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [TranslateModule.forRoot()],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideQueryClient(testQueryClient),
      ],
    });

    service = TestBed.inject(TakenService);
    zacHttpClient = TestBed.inject(ZacHttpClient);
    zacQueryClient = TestBed.inject(ZacQueryClient);
  });

  describe("readTaak", () => {
    it("fetches with the given task id", () => {
      jest.spyOn(zacHttpClient, "GET").mockReturnValue(of({} as never));
      service.readTaak("taak-1");
      expect(zacHttpClient.GET).toHaveBeenCalledWith(expect.any(String), {
        path: { taskId: "taak-1" },
      });
    });
  });

  describe("listTakenVoorZaak", () => {
    it("fetches with the given zaak UUID", () => {
      jest.spyOn(zacHttpClient, "GET").mockReturnValue(of([] as never));
      service.listTakenVoorZaak("zaak-uuid-1");
      expect(zacHttpClient.GET).toHaveBeenCalledWith(expect.any(String), {
        path: { zaakUUID: "zaak-uuid-1" },
      });
    });
  });

  describe("listTakenVoorZaakQuery", () => {
    it("builds query options for the given zaak UUID", () => {
      jest.spyOn(zacQueryClient, "GET");
      service.listTakenVoorZaakQuery("zaak-uuid-1");
      expect(zacQueryClient.GET).toHaveBeenCalledWith(expect.any(String), {
        path: { zaakUUID: "zaak-uuid-1" },
      });
    });
  });

  describe("invalidateTakenVoorZaak", () => {
    it("invalidates the taken listing of the given zaak", async () => {
      const invalidateQueries = jest
        .spyOn(testQueryClient, "invalidateQueries")
        .mockResolvedValue(undefined);

      await service.invalidateTakenVoorZaak("zaak-uuid-1");

      expect(invalidateQueries).toHaveBeenCalledWith({
        queryKey: service.listTakenVoorZaakQuery("zaak-uuid-1").queryKey,
      });
    });
  });

  describe("listHistorieVoorTaak", () => {
    it("fetches with the given task id", () => {
      jest.spyOn(zacHttpClient, "GET").mockReturnValue(of([] as never));
      service.listHistorieVoorTaak("taak-1");
      expect(zacHttpClient.GET).toHaveBeenCalledWith(expect.any(String), {
        path: { taskId: "taak-1" },
      });
    });
  });

  describe("toekennen", () => {
    it("builds mutation options for toekennen", () => {
      jest.spyOn(zacQueryClient, "PATCH");
      service.toekennen();
      expect(zacQueryClient.PATCH).toHaveBeenCalledWith(expect.any(String));
    });
  });

  describe("toekennenAanIngelogdeMedewerker", () => {
    it("builds mutation options for /rest/taken/toekennen/mij", () => {
      jest.spyOn(zacQueryClient, "PATCH");
      service.toekennenAanIngelogdeMedewerker();
      expect(zacQueryClient.PATCH).toHaveBeenCalledWith(
        "/rest/taken/toekennen/mij",
      );
    });

    it("names the behandelaar the taak was assigned to", async () => {
      const utilService = TestBed.inject(UtilService);
      jest.spyOn(utilService, "openSnackbar").mockImplementation(() => {});

      await runMutationOnSuccess(
        service.toekennenAanIngelogdeMedewerker(),
        undefined,
        fromPartial<GeneratedType<"RestTask">>({
          behandelaar: { naam: "fakeBehandelaarNaam" },
        }),
      );

      expect(utilService.openSnackbar).toHaveBeenCalledWith(
        "msg.taak.toegekend",
        { behandelaar: "fakeBehandelaarNaam" },
      );
    });
  });

  describe("toekennenAanIngelogdeMedewerkerVanuitLijst", () => {
    it("builds mutation options for /rest/taken/lijst/toekennen/mij", () => {
      jest.spyOn(zacQueryClient, "PATCH");
      service.toekennenAanIngelogdeMedewerkerVanuitLijst();
      expect(zacQueryClient.PATCH).toHaveBeenCalledWith(
        "/rest/taken/lijst/toekennen/mij",
      );
    });

    it("names the behandelaar the taak was assigned to", async () => {
      const utilService = TestBed.inject(UtilService);
      jest.spyOn(utilService, "openSnackbar").mockImplementation(() => {});

      await runMutationOnSuccess(
        service.toekennenAanIngelogdeMedewerkerVanuitLijst(),
        undefined,
        fromPartial<GeneratedType<"RestTask">>({
          behandelaar: { naam: "fakeBehandelaarNaam" },
        }),
      );

      expect(utilService.openSnackbar).toHaveBeenCalledWith(
        "msg.taak.toegekend",
        { behandelaar: "fakeBehandelaarNaam" },
      );
    });
  });

  describe("updateTaakdata", () => {
    it("builds mutation options for taakdata update", () => {
      jest.spyOn(zacQueryClient, "PUT");
      service.updateTaakdata();
      expect(zacQueryClient.PUT).toHaveBeenCalledWith(expect.any(String));
    });
  });

  describe("complete", () => {
    it("builds mutation options for complete", () => {
      jest.spyOn(zacQueryClient, "PATCH");
      service.complete();
      expect(zacQueryClient.PATCH).toHaveBeenCalledWith(expect.any(String));
    });

    describe("when the taak is completed", () => {
      let router: Router;

      beforeEach(() => {
        jest
          .spyOn(TestBed.inject(UtilService), "openSnackbar")
          .mockImplementation(() => {});
        router = TestBed.inject(Router);
        jest.spyOn(router, "navigateByUrl").mockResolvedValue(true);
      });

      it("returns to the page the taak was opened from", async () => {
        jest.spyOn(router, "url", "get").mockReturnValue("/taken/1");
        const zaakUrl = router.parseUrl("/zaken/ZAAK-1");
        jest.spyOn(router, "lastSuccessfulNavigation", "get").mockReturnValue(
          fromPartial({
            extras: {},
            previousNavigation: { finalUrl: zaakUrl },
          }),
        );

        await runMutationOnSuccess(service.complete());

        expect(router.navigateByUrl).toHaveBeenCalledWith(zaakUrl);
      });

      it("goes to the dashboard when the taak has no page it was opened from", async () => {
        jest.spyOn(router, "url", "get").mockReturnValue("/taken/1");
        jest
          .spyOn(router, "lastSuccessfulNavigation", "get")
          .mockReturnValue(null);

        await runMutationOnSuccess(service.complete());

        expect(router.navigateByUrl).toHaveBeenCalledWith("/");
      });

      it("stays on the taak when it was opened from an e-mail", async () => {
        jest.spyOn(router, "url", "get").mockReturnValue("/taken/1?bron=email");

        await runMutationOnSuccess(service.complete());

        expect(router.navigateByUrl).not.toHaveBeenCalled();
      });
    });
  });

  describe("verdelenVanuitLijst", () => {
    it("builds mutation options for verdelen", () => {
      jest.spyOn(zacQueryClient, "PUT");
      service.verdelenVanuitLijst();
      expect(zacQueryClient.PUT).toHaveBeenCalledWith(expect.any(String));
    });
  });

  describe("vrijgevenVanuitLijst", () => {
    it("builds mutation options for vrijgeven", () => {
      jest.spyOn(zacQueryClient, "PUT");
      service.vrijgevenVanuitLijst();
      expect(zacQueryClient.PUT).toHaveBeenCalledWith(expect.any(String));
    });
  });
});
