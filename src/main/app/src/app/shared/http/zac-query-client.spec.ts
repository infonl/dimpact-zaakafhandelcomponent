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
import { ZacQueryClient } from "./zac-query-client";

describe(`${ZacQueryClient.name}.PUT_QUERY`, () => {
  let zacQueryClient: ZacQueryClient;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [TranslateModule.forRoot()],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });

    zacQueryClient = TestBed.inject(ZacQueryClient);
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  it("keys the query on the endpoint, the body and the path parameters", () => {
    const query = zacQueryClient.PUT_QUERY(
      "/rest/signaleringen/zaken/{type}",
      { page: 0, rows: 5, sortField: "CREATED", sortOrder: "DESC" },
      { path: { type: "ZAAK_OP_NAAM" } },
    );

    expect(query.queryKey).toEqual([
      "/rest/signaleringen/zaken/{type}",
      { page: 0, rows: 5, sortField: "CREATED", sortOrder: "DESC" },
      { path: { type: "ZAAK_OP_NAAM" } },
    ]);
  });

  it("gives two sets of filters two cache entries", () => {
    const first = zacQueryClient.PUT_QUERY("/rest/zoeken/list", {
      page: 0,
      rows: 10,
    });
    const second = zacQueryClient.PUT_QUERY("/rest/zoeken/list", {
      page: 1,
      rows: 10,
    });

    expect(first.queryKey).not.toEqual(second.queryKey);
  });

  it("sends the body as a PUT and resolves with the response", async () => {
    const query = zacQueryClient.PUT_QUERY("/rest/zoeken/list", {
      page: 0,
      rows: 10,
    });

    const response = query.queryFn!({} as never);
    const request = httpTestingController.expectOne({
      method: "PUT",
      url: "/rest/zoeken/list",
    });
    expect(request.request.body).toEqual({ page: 0, rows: 10 });
    request.flush({ totaal: 0 });

    await expect(response).resolves.toEqual({ totaal: 0 });
  });
});
