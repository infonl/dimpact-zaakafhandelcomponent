/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  HttpEventType,
  HttpHeaderResponse,
  provideHttpClient,
  withInterceptorsFromDi,
} from "@angular/common/http";
import {
  HttpTestingController,
  provideHttpClientTesting,
} from "@angular/common/http/testing";
import { TestBed } from "@angular/core/testing";
import { TranslateModule } from "@ngx-translate/core";
import { expectTypeOf } from "expect-type";
import { paths } from "../../../generated/types/zac-openapi-types";
import { FoutAfhandelingService } from "../../fout-afhandeling/fout-afhandeling.service";
import { NullableIfOptional } from "../utils/generated-types";
import { HttpClient } from "./http-client";

describe(HttpClient.name, () => {
  let httpclient: HttpClient;
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
    httpclient = TestBed.inject(HttpClient);
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  describe(HttpClient.prototype.GET.name, () => {
    it("Replaces the path params", (done) => {
      const testData: paths["/rest/bag/zaak/{zaakUuid}"]["get"]["responses"]["200"]["content"]["application/json"] =
        [
          {
            uuid: "123",
            // etc.
          },
        ];

      httpclient
        .GET("/rest/bag/zaak/{zaakUuid}", {
          path: { zaakUuid: "123" },
        })
        .subscribe((data) => {
          expectTypeOf(data as typeof testData).toExtend<
            paths["/rest/bag/zaak/{zaakUuid}"]["get"]["responses"]["200"]["content"]["application/json"]
          >();

          expect(data).toEqual(testData);
          done();
        });

      const req = httpTestingController.expectOne("/rest/bag/zaak/123");
      expect(req.request.method).toEqual("GET");
      req.flush(testData);
      httpTestingController.verify();
    });

    it("adds the query params", (done) => {
      httpclient
        .PUT(
          "/rest/zaken/gekoppelde-zaken/{zaakUuid}/zoek-koppelbare-zaken",
          {
            zoekZaakIdentifier: "test",
            relationType: "HOOFDZAAK",
            page: 1,
            rows: 10,
          },
          {
            path: { zaakUuid: "123" },
          },
        )
        .subscribe(() => {
          done();
        });

      const req = httpTestingController.expectOne(
        "/rest/zaken/gekoppelde-zaken/123/zoek-koppelbare-zaken",
      );
      expect(req.request.method).toEqual("PUT");
      req.flush(null);
      httpTestingController.verify();
    });
  });

  describe(HttpClient.prototype.POST.name, () => {
    it("Http post works with all expected types", (done) => {
      httpclient
        .POST(
          "/rest/informatieobjecten/informatieobject/{uuid}/convert",
          undefined as never,
          {
            query: { zaak: "123" },
            path: { uuid: "123" },
          },
        )
        .subscribe(() => {
          done();
        });
      const req = httpTestingController.expectOne(
        "/rest/informatieobjecten/informatieobject/123/convert?zaak=123",
      );
      expect(req.request.method).toEqual("POST");
      req.flush(null, { status: 204, statusText: "No Content" });
      httpTestingController.verify();
    });
  });

  describe(HttpClient.prototype.POST_WITH_PROGRESS.name, () => {
    const path =
      "/rest/informatieobjecten/informatieobject/{zaakUuid}/{documentReferenceId}" as const;
    const parameters = {
      path: { zaakUuid: "zaak-1", documentReferenceId: "reference-1" },
      query: { taakObject: false },
    };

    it("reports how much of the document has been uploaded before reporting the response", (done) => {
      const emitted: unknown[] = [];

      httpclient
        .POST_WITH_PROGRESS(path, new FormData() as never, parameters)
        .subscribe({
          next: (progress) => emitted.push(progress),
          complete: () => {
            expect(emitted).toEqual([
              { state: "uploading", percentage: 0 },
              { state: "uploading", percentage: 25 },
              { state: "uploading", percentage: 100 },
              { state: "done", body: { uuid: "document-1" } },
            ]);
            done();
          },
        });

      const request = httpTestingController.expectOne(
        "/rest/informatieobjecten/informatieobject/zaak-1/reference-1?taakObject=false",
      );
      expect(request.request.reportProgress).toBe(true);
      request.event({
        type: HttpEventType.UploadProgress,
        loaded: 25,
        total: 100,
      });
      request.event({
        type: HttpEventType.UploadProgress,
        loaded: 100,
        total: 100,
      });
      request.flush({ uuid: "document-1" });
      httpTestingController.verify();
    });

    it("keeps the upload at 100% when the response events arrive, rather than falling back to 0%", (done) => {
      const emitted: unknown[] = [];

      httpclient
        .POST_WITH_PROGRESS(path, new FormData() as never, parameters)
        .subscribe({
          next: (progress) => emitted.push(progress),
          complete: () => {
            expect(emitted).toEqual([
              { state: "uploading", percentage: 0 },
              { state: "uploading", percentage: 100 },
              { state: "done", body: { uuid: "document-1" } },
            ]);
            done();
          },
        });

      const request = httpTestingController.expectOne(
        "/rest/informatieobjecten/informatieobject/zaak-1/reference-1?taakObject=false",
      );
      request.event({
        type: HttpEventType.UploadProgress,
        loaded: 100,
        total: 100,
      });
      request.event(new HttpHeaderResponse({ status: 200 }));
      request.event({
        type: HttpEventType.DownloadProgress,
        loaded: 10,
      });
      request.flush({ uuid: "document-1" });
      httpTestingController.verify();
    });

    it("reports no percentage while the total size of the document is still unknown", (done) => {
      const emitted: unknown[] = [];

      httpclient
        .POST_WITH_PROGRESS(path, new FormData() as never, parameters)
        .subscribe({
          next: (progress) => emitted.push(progress),
          complete: () => {
            expect(emitted).toContainEqual({
              state: "uploading",
              percentage: 0,
            });
            expect(emitted).not.toContainEqual(
              expect.objectContaining({ percentage: NaN }),
            );
            done();
          },
        });

      const request = httpTestingController.expectOne(
        "/rest/informatieobjecten/informatieobject/zaak-1/reference-1?taakObject=false",
      );
      request.event({ type: HttpEventType.UploadProgress, loaded: 25 });
      request.flush({ uuid: "document-1" });
    });
  });

  describe(HttpClient.prototype.PUT.name, () => {
    it("Http PUT works with all expected types", (done) => {
      const path =
        "/rest/gebruikersvoorkeuren/aantal-per-pagina/{werklijst}/{aantal}" as const;

      httpclient
        .PUT(path, undefined as never, {
          path: { aantal: 2, werklijst: "AFGEHANDELDE_ZAKEN" },
        })
        .subscribe((data) => {
          expectTypeOf<typeof data>().toBeNever();
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
      httpclient
        .PATCH(path, {} as never, {
          path: { uuid: "123" },
        })
        .subscribe((data) => {
          expectTypeOf(data).toExtend<
            NullableIfOptional<
              paths[typeof path]["patch"]["responses"]["200"]["content"]["application/json"]
            >
          >();
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

  describe(HttpClient.prototype.DELETE.name, () => {
    it("Http delete works with all expected types", (done) => {
      const path = "/rest/gebruikersvoorkeuren/zoekopdracht/{id}" as const;
      httpclient
        .DELETE(path, {
          path: { id: 123 },
        })
        .subscribe((data) => {
          expectTypeOf<typeof data>().toBeNever();
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
