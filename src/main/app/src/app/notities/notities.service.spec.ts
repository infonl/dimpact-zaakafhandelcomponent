/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HttpTestingController } from "@angular/common/http/testing";
import { TestBed } from "@angular/core/testing";
import { TranslateModule } from "@ngx-translate/core";
import { sleep, testQueryClient } from "../../../setupJest";
import { runMutation } from "../shared/http/run-mutation";
import { GeneratedType } from "../shared/utils/generated-types";
import { NotitieService } from "./notities.service";

describe(NotitieService.name, () => {
  let service: NotitieService;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [TranslateModule.forRoot()],
      providers: [],
    });

    service = TestBed.inject(NotitieService);
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  describe("createNotitie", () => {
    it("posts the notitie and returns the one that was created", async () => {
      const notitie: GeneratedType<"RestNote"> = {
        zaakUUID: "fakeZaakUuid",
        tekst: "fakeTekst",
        gebruikersnaamMedewerker: "fakeGebruikersnaam",
      };
      const createdNotitie: GeneratedType<"RestNote"> = { ...notitie, id: 1 };
      const onCreated = jest.fn();

      runMutation(testQueryClient, service.createNotitie(), notitie).subscribe(
        onCreated,
      );
      await sleep();

      const request = httpTestingController.expectOne("/rest/notities");
      expect(request.request.method).toBe("POST");
      expect(request.request.body).toEqual(notitie);
      request.flush(createdNotitie);
      await sleep();

      expect(onCreated).toHaveBeenCalledWith(createdNotitie);
    });
  });

  describe("updateNotitie", () => {
    it("patches the notitie and returns the one that was updated", async () => {
      const notitie: GeneratedType<"RestNote"> = {
        id: 1,
        zaakUUID: "fakeZaakUuid",
        tekst: "fakeTekst",
        gebruikersnaamMedewerker: "fakeGebruikersnaam",
      };
      const updatedNotitie: GeneratedType<"RestNote"> = {
        ...notitie,
        tijdstipLaatsteWijziging: "2026-10-07T12:00:00Z",
      };
      const onUpdated = jest.fn();

      runMutation(testQueryClient, service.updateNotitie(), notitie).subscribe(
        onUpdated,
      );
      await sleep();

      const request = httpTestingController.expectOne("/rest/notities");
      expect(request.request.method).toBe("PATCH");
      expect(request.request.body).toEqual(notitie);
      request.flush(updatedNotitie);
      await sleep();

      expect(onUpdated).toHaveBeenCalledWith(updatedNotitie);
    });
  });
});
