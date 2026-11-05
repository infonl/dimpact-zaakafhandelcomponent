/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HttpErrorResponse } from "@angular/common/http";
import { HttpTestingController } from "@angular/common/http/testing";
import { TestBed } from "@angular/core/testing";
import { provideTranslateService } from "@ngx-translate/core";
import { QueryClient } from "@tanstack/angular-query-experimental";
import { of } from "rxjs";
import { sleep, testQueryClient } from "../../../setupJest";
import { FoutAfhandelingService } from "../fout-afhandeling/fout-afhandeling.service";
import { QUERY_CLIENT } from "../shared/http/query-client";
import { runMutation } from "../shared/http/run-mutation";
import { InformatieObjectenService } from "./informatie-objecten.service";

describe(InformatieObjectenService.name, () => {
  const documentUuid = "document-uuid-1";
  const zaakUuid = "zaak-uuid-1";

  let service: InformatieObjectenService;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideTranslateService()],
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

  describe("the reads that a click or an event needs fresh", () => {
    const answers = [
      {
        read: "the zaken of a document",
        options: () =>
          service.listZaakIdentificatiesForInformatieobject(documentUuid),
        url: `/rest/informatieobjecten/informatieobject/${documentUuid}/zaakidentificaties`,
        answer: ["fakeZaakIdentificatie"],
      },
      {
        read: "the document of a zaakinformatieobject",
        options: () =>
          service.readEnkelvoudigInformatieobjectByZaakInformatieobjectUUID(
            "fakeZaakInformatieobjectUuid",
          ),
        url: "/rest/informatieobjecten/zaakinformatieobject/fakeZaakInformatieobjectUuid/informatieobject",
        answer: { titel: "fakeTitel" },
      },
      {
        read: "the edit link of a document",
        options: () =>
          service.editEnkelvoudigInformatieObjectInhoud(documentUuid, zaakUuid),
        url: `/rest/informatieobjecten/informatieobject/${documentUuid}/edit?zaak=${zaakUuid}`,
        answer: "https://example.com/edit",
      },
    ];

    async function readOnce({
      options,
      url,
      answer,
    }: (typeof answers)[number]) {
      const read = testQueryClient.query(options() as never);
      await sleep();
      httpTestingController.expectOne(url).flush(answer as never);
      await read;
    }

    it.each(answers)("asks the server every time for $read", async (entry) => {
      await readOnce(entry);
      await sleep();

      await readOnce(entry);
    });

    it.each(answers)("keeps no copy of $read after the read", async (entry) => {
      await readOnce(entry);
      await sleep();

      expect(
        testQueryClient.getQueryState(entry.options().queryKey),
      ).toBeUndefined();
    });

    it("does not retry the edit link, as a browser only allows window.open shortly after the click", async () => {
      const read = testQueryClient.query(
        service.editEnkelvoudigInformatieObjectInhoud(documentUuid, zaakUuid),
      );
      await sleep();

      httpTestingController
        .expectOne(
          `/rest/informatieobjecten/informatieobject/${documentUuid}/edit?zaak=${zaakUuid}`,
        )
        .flush(null, { status: 500, statusText: "Internal Server Error" });

      await expect(read).rejects.toBeDefined();
    });
  });

  describe(
    InformatieObjectenService.prototype.invalidateInformatieobject.name,
    () => {
      const otherDocumentUuid = "document-uuid-2";

      function seed(queryKey: readonly unknown[]) {
        testQueryClient.setQueryData(queryKey, {});
      }

      function isInvalidated(queryKey: readonly unknown[]) {
        return testQueryClient.getQueryState(queryKey)?.isInvalidated;
      }

      it("marks every read of the document as stale, so that the next read goes to the server", async () => {
        const reads = [
          service.readEnkelvoudigInformatieobject(documentUuid).queryKey,
          service.readEnkelvoudigInformatieobjectVersie(documentUuid, 1)
            .queryKey,
          service.readHuidigeVersieEnkelvoudigInformatieObject(documentUuid)
            .queryKey,
          service.listZaakInformatieobjecten(documentUuid).queryKey,
          service.listHistorie(documentUuid).queryKey,
        ];
        reads.forEach(seed);

        await service.invalidateInformatieobject(documentUuid);

        expect(reads.map(isInvalidated)).toEqual(reads.map(() => true));
      });

      it("leaves the reads of another document alone", async () => {
        const otherRead =
          service.readEnkelvoudigInformatieobject(otherDocumentUuid).queryKey;
        seed(otherRead);

        await service.invalidateInformatieobject(documentUuid);

        expect(isInvalidated(otherRead)).toBe(false);
      });
    },
  );
});

describe(`${InformatieObjectenService.name} when a read of the document fails`, () => {
  const documentUuid = "document-uuid-1";
  const documentUrl = `/rest/informatieobjecten/informatieobject/${documentUuid}`;
  const retryDelays = [1000, 2000, 4000];

  let service: InformatieObjectenService;
  let httpTestingController: HttpTestingController;
  let queryClient: QueryClient;
  let foutAfhandelen: jest.SpyInstance;

  beforeEach(() => {
    jest.useFakeTimers();
    TestBed.configureTestingModule({
      providers: [
        provideTranslateService(),
        { provide: QueryClient, useExisting: QUERY_CLIENT },
      ],
    });

    service = TestBed.inject(InformatieObjectenService);
    httpTestingController = TestBed.inject(HttpTestingController);
    queryClient = TestBed.inject(QueryClient);
    foutAfhandelen = jest
      .spyOn(TestBed.inject(FoutAfhandelingService), "foutAfhandelen")
      .mockReturnValue(of());
  });

  afterEach(() => {
    queryClient.clear();
    jest.useRealTimers();
  });

  function readDocument() {
    const read = queryClient.query(
      service.readEnkelvoudigInformatieobject(documentUuid),
    );
    read.catch(() => undefined);
    return read;
  }

  function answerWith(status: number, statusText: string) {
    httpTestingController
      .expectOne(documentUrl)
      .flush(null, { status, statusText });
  }

  it("retries a server fault three times before it opens the error dialog once", async () => {
    const read = readDocument();
    await jest.advanceTimersByTimeAsync(0);
    answerWith(500, "Internal Server Error");

    for (const delay of retryDelays) {
      await jest.advanceTimersByTimeAsync(delay);
      answerWith(500, "Internal Server Error");
    }

    await expect(read).rejects.toBeInstanceOf(HttpErrorResponse);
    expect(foutAfhandelen).toHaveBeenCalledTimes(1);
  });

  it("does not retry a read the server refuses, because a retry would not change its answer", async () => {
    const read = readDocument();
    await jest.advanceTimersByTimeAsync(0);
    answerWith(404, "Not Found");

    await expect(read).rejects.toBeInstanceOf(HttpErrorResponse);
    await jest.advanceTimersByTimeAsync(10_000);
    httpTestingController.expectNone(documentUrl);
    expect(foutAfhandelen).toHaveBeenCalledTimes(1);
  });

  it("opens the error dialog when the re-read after an invalidation fails, because the user waits on that read while the old copy is still on screen", async () => {
    const firstRead = readDocument();
    await jest.advanceTimersByTimeAsync(0);
    httpTestingController.expectOne(documentUrl).flush({ uuid: documentUuid });
    await firstRead;
    await service.invalidateInformatieobject(documentUuid);

    const reRead = readDocument();
    await jest.advanceTimersByTimeAsync(0);
    answerWith(404, "Not Found");

    await expect(reRead).rejects.toBeInstanceOf(HttpErrorResponse);
    expect(foutAfhandelen).toHaveBeenCalledTimes(1);
  });
});
