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
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { sleep, testQueryClient } from "../../../setupJest";
import { fromPartial } from "../../test-helpers";
import { runMutation } from "../shared/http/run-mutation";
import { GeneratedType } from "../shared/utils/generated-types";
import { GebruikersvoorkeurenService } from "./gebruikersvoorkeuren.service";

describe(GebruikersvoorkeurenService.name, () => {
  let service: GebruikersvoorkeurenService;
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

    service = TestBed.inject(GebruikersvoorkeurenService);
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  describe("setZoekopdrachtActief", () => {
    it("puts the zoekopdracht that became active", async () => {
      const zoekopdracht = fromPartial<GeneratedType<"RESTZoekopdracht">>({
        id: 42,
      });

      runMutation(
        testQueryClient,
        service.setZoekopdrachtActief(),
        zoekopdracht,
      ).subscribe();
      await sleep();

      const request = httpTestingController.expectOne(
        "/rest/gebruikersvoorkeuren/zoekopdracht/actief",
      );
      expect(request.request.method).toBe("PUT");
      expect(request.request.body).toEqual(zoekopdracht);
      request.flush(null);
    });
  });

  describe("updateAantalPerPagina", () => {
    it("addresses the werklijst and page size by path", async () => {
      runMutation(
        testQueryClient,
        service.updateAantalPerPagina("MIJN_ZAKEN", 50),
        undefined as never,
      ).subscribe();
      await sleep();

      const request = httpTestingController.expectOne(
        "/rest/gebruikersvoorkeuren/aantal-per-pagina/MIJN_ZAKEN/50",
      );
      expect(request.request.method).toBe("PUT");
      request.flush(null);
    });
  });

  describe("addDashboardCard", () => {
    it("puts the card instelling", async () => {
      const instelling = fromPartial<
        GeneratedType<"RESTDashboardCardInstelling">
      >({ column: 1, row: 2 });

      runMutation(
        testQueryClient,
        service.addDashboardCard(),
        instelling,
      ).subscribe();
      await sleep();

      const request = httpTestingController.expectOne(
        "/rest/gebruikersvoorkeuren/dasboardcard",
      );
      expect(request.request.method).toBe("PUT");
      expect(request.request.body).toEqual(instelling);
      request.flush([]);
    });
  });

  describe("updateDashboardCards", () => {
    it("puts the whole set of card instellingen", async () => {
      const instellingen = [
        fromPartial<GeneratedType<"RESTDashboardCardInstelling">>({
          column: 0,
          row: 0,
        }),
      ];

      runMutation(
        testQueryClient,
        service.updateDashboardCards(),
        instellingen,
      ).subscribe();
      await sleep();

      const request = httpTestingController.expectOne(
        "/rest/gebruikersvoorkeuren/dasboardcard/actief",
      );
      expect(request.request.method).toBe("PUT");
      expect(request.request.body).toEqual(instellingen);
      request.flush([]);
    });
  });
});
