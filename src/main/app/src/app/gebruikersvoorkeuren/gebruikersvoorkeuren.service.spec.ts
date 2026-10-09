/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HttpTestingController } from "@angular/common/http/testing";
import { TestBed } from "@angular/core/testing";
import { TranslateModule } from "@ngx-translate/core";
import { type MutationFunctionContext } from "@tanstack/angular-query-experimental";
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
      providers: [],
    });

    service = TestBed.inject(GebruikersvoorkeurenService);
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  describe("createOrUpdateZoekOpdrachten", () => {
    it("posts the zoekopdracht to save", async () => {
      const zoekopdracht = fromPartial<GeneratedType<"RESTZoekopdracht">>({
        naam: "mijn zoekopdracht",
        json: "{}",
        lijstID: "WERKVOORRAAD_ZAKEN",
      });

      runMutation(
        testQueryClient,
        service.createOrUpdateZoekOpdrachten(),
        zoekopdracht,
      ).subscribe();
      await sleep();

      const request = httpTestingController.expectOne(
        "/rest/gebruikersvoorkeuren/zoekopdracht",
      );
      expect(request.request.method).toBe("POST");
      expect(request.request.body).toEqual(zoekopdracht);
      request.flush(zoekopdracht);
    });
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

  describe("deleteZoekOpdrachten", () => {
    it("addresses the zoekopdracht by its id", async () => {
      const request = service.deleteZoekOpdrachten().mutationFn!(
        7,
        fromPartial<MutationFunctionContext>({}),
      );
      httpTestingController
        .expectOne("/rest/gebruikersvoorkeuren/zoekopdracht/7")
        .flush(null);

      await request;
    });
  });

  describe("removeZoekopdrachtActief", () => {
    it("addresses the werklijst the zoekopdracht is active for", async () => {
      const request = service.removeZoekopdrachtActief().mutationFn!(
        "MIJN_ZAKEN",
        fromPartial<MutationFunctionContext>({}),
      );
      httpTestingController
        .expectOne("/rest/gebruikersvoorkeuren/zoekopdracht/MIJN_ZAKEN/actief")
        .flush(null);

      await request;
    });
  });
});
