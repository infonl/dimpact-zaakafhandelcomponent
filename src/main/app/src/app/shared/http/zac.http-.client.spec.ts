/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

// Http testing module and mocking controller
import {
  HttpClientTestingModule,
  HttpTestingController,
} from "@angular/common/http/testing";
import { expectType } from "ts-expect";
// Other imports
import { TestBed } from "@angular/core/testing";

import { Paths, ZacHttpClient } from "./zac-http-client";

describe("HttpClientTesting", () => {
  let zacHttpClient: ZacHttpClient;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
    });

    zacHttpClient = TestBed.inject(ZacHttpClient);
    httpTestingController = TestBed.inject(HttpTestingController);
  });
  afterEach(() => {
    httpTestingController.verify();
  });

  it("Http get works with all expected types", (done) => {
    const testData: Paths["/rest/bag/zaak/{zaakUuid}"]["get"]["responses"]["200"]["content"]["application/json"] =
      [
        {
          uuid: "123",
          // etc.
        },
      ];

    zacHttpClient
      .GET("/rest/bag/zaak/{zaakUuid}", {
        pathParams: { path: { zaakUuid: "123" } },
      })
      .subscribe((data) => {
        expectType<
          Paths["/rest/bag/zaak/{zaakUuid}"]["get"]["responses"]["200"]["content"]["application/json"]
        >(data);

        expect(data).toEqual(testData);
        done();
      });
    const req = httpTestingController.expectOne("/rest/bag/zaak/123");
    expect(req.request.method).toEqual("GET");
    req.flush(testData);
    httpTestingController.verify();
  });

  it("Http post works with all expected types", (done) => {
    const testData: Paths["/rest/informatieobjecten/informatieobject/{uuid}/convert"]["post"]["responses"]["200"] =
      {
        headers: {},
        content: undefined as never,
      };

    zacHttpClient
      .POST(
        "/rest/informatieobjecten/informatieobject/{uuid}/convert",
        undefined as never,
        {
          pathParams: { query: { zaak: "123" }, path: { uuid: "123" } },
        },
      )
      .subscribe((data) => {
        expectType<
          Paths["/rest/informatieobjecten/informatieobject/{uuid}/convert"]["post"]["responses"]["200"]["content"]["application/json"]
        >(data);

        expect(data).toEqual(testData);
        done();
      });
    const req = httpTestingController.expectOne(
      "/rest/informatieobjecten/informatieobject/123/convert",
    );
    expect(req.request.method).toEqual("POST");
    req.flush(testData);
    httpTestingController.verify();
  });

  it("Http delete works with all expected types", (done) => {
    const path = "/rest/gebruikersvoorkeuren/zoekopdracht/{id}" as const;
    zacHttpClient
      .DELETE(path, {
        pathParams: { path: { id: 123 } },
      })
      .subscribe((data) => {
        expectType<Paths[typeof path]["delete"]["responses"]["204"]["content"]>(
          data,
        );
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

  it("Http PUT works with all expected types", (done) => {
    const path =
      "/rest/gebruikersvoorkeuren/aantal-per-pagina/{werklijst}/{aantal}" as const;
    zacHttpClient
      .PUT(path, {} as never, {
        pathParams: { path: { aantal: 2, werklijst: "AFGEHANDELDE_ZAKEN" } },
      })
      .subscribe((data) => {
        expectType<Paths[typeof path]["put"]["responses"]["204"]["content"]>(
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
      Paths[typeof path]["patch"]["responses"]["200"]["content"]["application/json"]
    > = { uuid: "123" };
    zacHttpClient
      .PATCH(path, {} as never, {
        pathParams: { path: { uuid: "123" } },
      })
      .subscribe((data) => {
        expectType<
          Paths[typeof path]["patch"]["responses"]["200"]["content"]["application/json"]
        >(data);
        expect(data).toEqual(testData);
        done();
      });
    const req = httpTestingController.expectOne("/rest/zaken/123/zaaklocatie");
    expect(req.request.method).toEqual("PATCH");
    req.flush(testData);
    httpTestingController.verify();
  });
});
