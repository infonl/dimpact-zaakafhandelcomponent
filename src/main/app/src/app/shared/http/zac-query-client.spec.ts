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
import { of } from "rxjs";
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

  afterEach(() => {
    httpTestingController.verify();
  });

  describe("DELETE", () => {
    describe("an endpoint addressed by a path parameter", () => {
      it("takes the parameter from the variables it is mutated with", async () => {
        const options = zacQueryClient.DELETE(
          "/rest/notities/{id}",
          (id: number) => ({ parameters: { path: { id } } }),
        );

        const response = options.mutationFn!(42, expect.anything());
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
          expect.anything(),
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
          expect.anything(),
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
        expect.anything(),
      );

      expect(foutAfhandelen).toHaveBeenCalled();
    });
  });
});
