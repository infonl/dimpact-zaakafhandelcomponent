/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HttpTestingController } from "@angular/common/http/testing";
import { TestBed } from "@angular/core/testing";
import { TranslateModule } from "@ngx-translate/core";
import { of } from "rxjs";
import { sleep, testQueryClient } from "../../../setupJest";
import { FoutAfhandelingService } from "../fout-afhandeling/fout-afhandeling.service";
import { runMutation } from "../shared/http/run-mutation";
import { InformatieObjectenService } from "./informatie-objecten.service";

describe(InformatieObjectenService.name, () => {
  const documentUuid = "document-uuid-1";
  const zaakUuid = "zaak-uuid-1";

  let service: InformatieObjectenService;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [TranslateModule.forRoot()],
      providers: [],
    });

    service = TestBed.inject(InformatieObjectenService);
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  describe.each([
    {
      action: "lockInformatieObject",
      endpoint: "lock",
      mutation: () => service.lockInformatieObject(documentUuid, zaakUuid),
    },
    {
      action: "unlockInformatieObject",
      endpoint: "unlock",
      mutation: () => service.unlockInformatieObject(documentUuid, zaakUuid),
    },
    {
      action: "ondertekenInformatieObject",
      endpoint: "onderteken",
      mutation: () =>
        service.ondertekenInformatieObject(documentUuid, zaakUuid),
    },
    {
      action: "convertInformatieObjectToPDF",
      endpoint: "convert",
      mutation: () =>
        service.convertInformatieObjectToPDF(documentUuid, zaakUuid),
    },
  ])("$action", ({ endpoint, mutation }) => {
    it("posts without a body to the document, scoped to its zaak", async () => {
      runMutation(testQueryClient, mutation(), undefined as never).subscribe();
      await sleep();

      const request = httpTestingController.expectOne(
        `/rest/informatieobjecten/informatieobject/${documentUuid}/${endpoint}?zaak=${zaakUuid}`,
      );
      expect(request.request.method).toBe("POST");
      expect(request.request.body).toBeNull();
      request.flush(null);
    });

    it("reports a failure through the error handler", async () => {
      const foutAfhandelen = jest
        .spyOn(TestBed.inject(FoutAfhandelingService), "foutAfhandelen")
        .mockReturnValue(of());

      runMutation(testQueryClient, mutation(), undefined as never).subscribe({
        error: () => undefined,
      });
      await sleep();

      httpTestingController
        .expectOne(
          `/rest/informatieobjecten/informatieobject/${documentUuid}/${endpoint}?zaak=${zaakUuid}`,
        )
        .flush(null, { status: 500, statusText: "Server Error" });
      await sleep();

      expect(foutAfhandelen).toHaveBeenCalled();
    });
  });

  describe("unlockInformatieObject", () => {
    it("leaves the zaak out of the query for a document without a zaak", async () => {
      runMutation(
        testQueryClient,
        service.unlockInformatieObject(documentUuid),
        undefined as never,
      ).subscribe();
      await sleep();

      const request = httpTestingController.expectOne(({ url }) =>
        url.startsWith(
          `/rest/informatieobjecten/informatieobject/${documentUuid}/unlock`,
        ),
      );
      expect(request.request.method).toBe("POST");
      expect(request.request.url).not.toContain("zaak=");
      request.flush(null);
    });
  });

  describe("getZIPDownload", () => {
    it("posts the document uuids and resolves with the zip as a blob", async () => {
      const zip = new Blob(["zip"], { type: "application/zip" });
      const onDownloaded = jest.fn();

      runMutation(testQueryClient, service.getZIPDownload(), [
        documentUuid,
      ]).subscribe(onDownloaded);
      await sleep();

      const request = httpTestingController.expectOne(
        "/rest/informatieobjecten/download/zip",
      );
      expect(request.request.method).toBe("POST");
      expect(request.request.body).toEqual([documentUuid]);
      expect(request.request.responseType).toBe("blob");
      request.flush(zip);
      await sleep();

      expect(onDownloaded).toHaveBeenCalledWith(zip);
    });
  });
});
