/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
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
import { expectType } from "ts-expect";
import { paths } from "../../../generated/types/zac-openapi-types";
import { FoutAfhandelingService } from "../../fout-afhandeling/fout-afhandeling.service";
import { ZacHttpClient } from "./zac-http-client";

describe(ZacHttpClient.name, () => {
  let zacHttpClient: ZacHttpClient;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [TranslateModule.forRoot()],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
      ],
    });

    TestBed.inject(FoutAfhandelingService);
    zacHttpClient = TestBed.inject(ZacHttpClient);
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe(ZacHttpClient.prototype.GET.name, () => {
    it("Replaces the path params", (done) => {
      const testData: paths["/rest/bag/zaak/{zaakUuid}"]["get"]["responses"]["200"]["content"]["application/json"] =
        [
          {
            uuid: "123",
            // etc.
          },
        ];

      zacHttpClient
        .GET("/rest/bag/zaak/{zaakUuid}", {
          path: { zaakUuid: "123" },
        })
        .subscribe((data) => {
          expectType<
            paths["/rest/bag/zaak/{zaakUuid}"]["get"]["responses"]["200"]["content"]["application/json"]
          >(data);

          expect(data).toEqual(testData);
          done();
        });

      const req = httpTestingController.expectOne("/rest/bag/zaak/123");
      expect(req.request.method).toEqual("GET");
      req.flush(testData);
      httpTestingController.verify();
    });

    it("adds the query params", (done) => {
      zacHttpClient
        .GET("/rest/zaken/gekoppelde-zaken/{zaakUuid}/zoek-koppelbare-zaken", {
          query: { zoekZaakIdentifier: "test", relationType: "HOOFDZAAK" },
          path: { zaakUuid: "123" },
        })
        .subscribe(() => {
          done();
        });

      const req = httpTestingController.expectOne(
        "/rest/zaken/gekoppelde-zaken/123/zoek-koppelbare-zaken?zoekZaakIdentifier=test&relationType=HOOFDZAAK",
      );
      expect(req.request.method).toEqual("GET");
      req.flush(null);
      httpTestingController.verify();
    });
  });

  describe(ZacHttpClient.prototype.POST.name, () => {
    it("Http post works with all expected types", (done) => {
      const testData: paths["/rest/informatieobjecten/informatieobject/{uuid}/convert"]["post"]["responses"]["200"] =
        {
          headers: {},
        };

      zacHttpClient
        .POST(
          "/rest/informatieobjecten/informatieobject/{uuid}/convert",
          undefined as never,
          {
            query: { zaak: "123" },
            path: { uuid: "123" },
          },
        )
        .subscribe((data) => {
          expectType<
            paths["/rest/informatieobjecten/informatieobject/{uuid}/convert"]["post"]["responses"]["200"]["content"]
          >(data);

          expect(data).toEqual(testData);
          done();
        });
      const req = httpTestingController.expectOne(
        "/rest/informatieobjecten/informatieobject/123/convert?zaak=123",
      );
      expect(req.request.method).toEqual("POST");
      req.flush(testData);
      httpTestingController.verify();
    });
  });

  describe(ZacHttpClient.prototype.PUT.name, () => {
    it("Http PUT works with all expected types", (done) => {
      const path =
        "/rest/gebruikersvoorkeuren/aantal-per-pagina/{werklijst}/{aantal}" as const;

      zacHttpClient
        .PUT(path, undefined as never, {
          path: { aantal: 2, werklijst: "AFGEHANDELDE_ZAKEN" },
        })
        .subscribe((data) => {
          expectType<paths[typeof path]["put"]["responses"]["204"]["content"]>(
            data,
          );
          expect(data).toBe(true);
          done();
        });
      const req = httpTestingController.expectOne(
        "/rest/gebruikersvoorkeuren/aantal-per-pagina/AFGEHANDELDE_ZAKEN/2",
      );
      expect(req.request.method).toEqual("PUT");
      req.flush(true);
      httpTestingController.verify();
    });

    it("Http PUT works with all expected types", (done) => {
      const path = "/rest/zaken/{uuid}/zaaklocatie" as const;
      const testData: Partial<
        paths[typeof path]["patch"]["responses"]["200"]["content"]["application/json"]
      > = { uuid: "123" };
      zacHttpClient
        .PATCH(path, {} as never, {
          path: { uuid: "123" },
        })
        .subscribe((data) => {
          expectType<
            paths[typeof path]["patch"]["responses"]["200"]["content"]["application/json"]
          >(data);
          expect(data).toEqual(testData);
          done();
        });
      const req = httpTestingController.expectOne(
        "/rest/zaken/123/zaaklocatie",
      );
      expect(req.request.method).toEqual("PATCH");
      req.flush(testData);
      httpTestingController.verify();
    });
  });

  describe(ZacHttpClient.prototype.DELETE.name, () => {
    it("Http delete works with all expected types", (done) => {
      const path = "/rest/gebruikersvoorkeuren/zoekopdracht/{id}" as const;
      zacHttpClient
        .DELETE(path, {
          path: { id: 123 },
        })
        .subscribe((data) => {
          expectType<
            paths[typeof path]["delete"]["responses"]["204"]["content"]
          >(data);
          expect(data).toBe(true);
          done();
        });
      const req = httpTestingController.expectOne(
        "/rest/gebruikersvoorkeuren/zoekopdracht/123",
      );
      expect(req.request.method).toEqual("DELETE");
      req.flush(true);
      httpTestingController.verify();
    });
  });
});
