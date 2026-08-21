/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  provideHttpClient,
  withInterceptorsFromDi,
} from "@angular/common/http";
import {
  HttpTestingController,
  provideHttpClientTesting,
} from "@angular/common/http/testing";
import { TestBed } from "@angular/core/testing";
import { TranslateModule } from "@ngx-translate/core";
import type { MutationFunctionContext } from "@tanstack/angular-query-experimental";
import { of } from "rxjs";
import { fromPartial } from "../../../test-helpers";
import { FoutAfhandelingService } from "../../fout-afhandeling/fout-afhandeling.service";
import { ZacQueryClient } from "./zac-query-client";

describe(ZacQueryClient.name, () => {
  let zacQueryClient: ZacQueryClient;
  let httpTestingController: HttpTestingController;
  let foutAfhandelingService: FoutAfhandelingService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [TranslateModule.forRoot()],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
      ],
    });

    zacQueryClient = TestBed.inject(ZacQueryClient);
    httpTestingController = TestBed.inject(HttpTestingController);
    foutAfhandelingService = TestBed.inject(FoutAfhandelingService);
  });

  describe("DELETE", () => {
    describe("an endpoint addressed by a path parameter", () => {
      it("takes the parameter from the variables it is mutated with", async () => {
        const options = zacQueryClient.DELETE(
          "/rest/notities/{id}",
          (id: number) => ({ parameters: { path: { id } } }),
        );

        const response = options.mutationFn!(
          42,
          fromPartial<MutationFunctionContext>({}),
        );
        httpTestingController.expectOne("/rest/notities/42").flush(null);

        await response;
      });

      it("sends the body the request derives from those same variables", async () => {
        const options = zacQueryClient.DELETE(
          "/rest/zaken/{uuid}/initiator",
          ({ zaakUuid, reden }: { zaakUuid: string; reden: string }) => ({
            parameters: { path: { uuid: zaakUuid } },
            body: { reden },
          }),
        );

        const response = options.mutationFn!(
          { zaakUuid: "fakeZaakUuid", reden: "fakeReden" },
          fromPartial<MutationFunctionContext>({}),
        );
        const request = httpTestingController.expectOne(
          "/rest/zaken/fakeZaakUuid/initiator",
        );
        request.flush(null);

        expect(request.request.body).toEqual({ reden: "fakeReden" });
        await response;
      });
    });

    describe("an endpoint without path parameters", () => {
      it("sends the variables as the request body", async () => {
        const options = zacQueryClient.DELETE("/rest/bag");

        const response = options.mutationFn!(
          { zaakUuid: "fakeZaakUuid" },
          fromPartial<MutationFunctionContext>({}),
        );
        const request = httpTestingController.expectOne("/rest/bag");
        request.flush(null);

        expect(request.request.body).toEqual({ zaakUuid: "fakeZaakUuid" });
        await response;
      });
    });

    it("reports a failure through the error handling", () => {
      const foutAfhandelen = jest
        .spyOn(foutAfhandelingService, "foutAfhandelen")
        .mockReturnValue(of());
      const options = zacQueryClient.DELETE(
        "/rest/notities/{id}",
        (id: number) => ({
          parameters: { path: { id } },
        }),
      );

      options.onError!(
        { status: 500 } as never,
        42 as never,
        undefined,
        fromPartial<MutationFunctionContext>({}),
      );

      expect(foutAfhandelen).toHaveBeenCalled();
    });
  });

  describe("PUT_QUERY", () => {
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
});
