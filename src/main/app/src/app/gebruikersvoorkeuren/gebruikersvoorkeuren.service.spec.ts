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
import {
  type MutationFunctionContext,
  provideQueryClient,
} from "@tanstack/angular-query-experimental";
import { testQueryClient } from "../../../setupJest";
import { fromPartial } from "../../test-helpers";
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

  afterEach(() => {
    httpTestingController.verify();
    testQueryClient.clear();
    jest.clearAllMocks();
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
