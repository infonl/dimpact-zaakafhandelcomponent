/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  provideHttpClient,
  withInterceptorsFromDi,
} from "@angular/common/http";
import { TestBed } from "@angular/core/testing";
import { TranslateModule } from "@ngx-translate/core";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { of } from "rxjs";
import { testQueryClient } from "../../../setupJest";
import { ZacHttpClient } from "../shared/http/zac-http-client";
import { ZacQueryClient } from "../shared/http/zac-query-client";
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

  afterEach(() => {
    testQueryClient.clear();
    jest.clearAllMocks();
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
    it("patches with the given body", () => {
      const body = {
        taakId: "taak-1",
        zaakUuid: "zaak-uuid-1",
        groepId: "groep-1",
      };
      jest.spyOn(zacHttpClient, "PATCH").mockReturnValue(of({} as never));
      service.toekennenAanIngelogdeMedewerker(body).subscribe();
      expect(zacHttpClient.PATCH).toHaveBeenCalledWith(
        expect.any(String),
        body,
      );
    });
  });

  describe("toekennenAanIngelogdeMedewerkerVanuitLijst", () => {
    it("patches with the given body", () => {
      const body = {
        taakId: "taak-1",
        zaakUuid: "zaak-uuid-1",
        groepId: "groep-1",
      };
      jest.spyOn(zacHttpClient, "PATCH").mockReturnValue(of({} as never));
      service.toekennenAanIngelogdeMedewerkerVanuitLijst(body).subscribe();
      expect(zacHttpClient.PATCH).toHaveBeenCalledWith(
        expect.any(String),
        body,
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
  });

  describe("verdelenVanuitLijst", () => {
    it("builds mutation options for verdelen", () => {
      jest.spyOn(zacQueryClient, "PUT");
      service.verdelenVanuitLijst();
      expect(zacQueryClient.PUT).toHaveBeenCalledWith(expect.any(String));
    });
  });

  describe("vrijgevenVanuitLijst", () => {
    it("puts with the given body", () => {
      const body = {
        taken: [{ taakId: "taak-1", zaakUuid: "zaak-uuid-1" }],
        reden: "test",
      };
      jest.spyOn(zacHttpClient, "PUT").mockReturnValue(of([] as never));
      service.vrijgevenVanuitLijst(body).subscribe();
      expect(zacHttpClient.PUT).toHaveBeenCalledWith(expect.any(String), body);
    });
  });
});
