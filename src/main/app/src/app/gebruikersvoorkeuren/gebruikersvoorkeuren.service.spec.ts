/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HttpTestingController } from "@angular/common/http/testing";
import { TestBed } from "@angular/core/testing";
import { provideTranslateService } from "@ngx-translate/core";
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
      providers: [provideTranslateService()],
    });

    service = TestBed.inject(GebruikersvoorkeurenService);
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  describe("createOrUpdateZoekOpdrachten", () => {
    it("posts the zoekopdracht to save", async () => {
      const zoekopdracht = fromPartial<GeneratedType<"RestZoekopdracht">>({
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
      const zoekopdracht = fromPartial<GeneratedType<"RestZoekopdracht">>({
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

    it("marks the cached tabel gegevens of the werklijst as stale, so the werklijst opens with the new page size next time", async () => {
      testQueryClient.setQueryData(
        service.readTabelGegevens("MIJN_ZAKEN").queryKey,
        fromPartial<GeneratedType<"RestTabelGegevens">>({
          aantalPerPagina: 25,
        }),
      );

      runMutation(
        testQueryClient,
        service.updateAantalPerPagina("MIJN_ZAKEN", 50),
        undefined as never,
      ).subscribe();
      await sleep();
      httpTestingController
        .expectOne("/rest/gebruikersvoorkeuren/aantal-per-pagina/MIJN_ZAKEN/50")
        .flush(null);
      await sleep();

      expect(
        testQueryClient.getQueryCache().find({
          queryKey: service.readTabelGegevens("MIJN_ZAKEN").queryKey,
        })?.state.isInvalidated,
      ).toBe(true);
    });
  });

  describe("listDashboardCards", () => {
    const instellingen = [
      fromPartial<GeneratedType<"RestDashboardCardInstelling">>({
        cardId: "MIJN_TAKEN",
        column: 0,
        row: 0,
      }),
    ];

    async function cachedDashboardCards() {
      await sleep();
      return testQueryClient.getQueryData(
        service.listDashboardCards().queryKey,
      );
    }

    it("caches the card instellingen that adding a card returns", async () => {
      runMutation(
        testQueryClient,
        service.addDashboardCard(),
        instellingen[0],
      ).subscribe();
      await sleep();
      httpTestingController
        .expectOne("/rest/gebruikersvoorkeuren/dasboardcard")
        .flush(instellingen);

      expect(await cachedDashboardCards()).toEqual(instellingen);
    });

    it("caches the card instellingen that updating the cards returns", async () => {
      runMutation(
        testQueryClient,
        service.updateDashboardCards(),
        instellingen,
      ).subscribe();
      await sleep();
      httpTestingController
        .expectOne("/rest/gebruikersvoorkeuren/dasboardcard/actief")
        .flush(instellingen);

      expect(await cachedDashboardCards()).toEqual(instellingen);
    });

    it("caches the card instellingen that deleting a card returns", async () => {
      runMutation(
        testQueryClient,
        service.deleteDashboardCard(),
        instellingen[0],
      ).subscribe();
      await sleep();
      httpTestingController
        .expectOne("/rest/gebruikersvoorkeuren/dasboardcard")
        .flush(instellingen);

      expect(await cachedDashboardCards()).toEqual(instellingen);
    });
  });

  describe("addDashboardCard", () => {
    it("puts the card instelling", async () => {
      const instelling = fromPartial<
        GeneratedType<"RestDashboardCardInstelling">
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
        fromPartial<GeneratedType<"RestDashboardCardInstelling">>({
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
