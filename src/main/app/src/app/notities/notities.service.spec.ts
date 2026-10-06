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
import { runMutation } from "../shared/http/run-mutation";
import { GeneratedType } from "../shared/utils/generated-types";
import { NotitieService } from "./notities.service";

describe(NotitieService.name, () => {
  let service: NotitieService;
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

    service = TestBed.inject(NotitieService);
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTestingController.verify();
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
});
