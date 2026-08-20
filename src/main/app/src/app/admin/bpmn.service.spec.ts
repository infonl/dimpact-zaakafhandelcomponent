/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HttpResponse, provideHttpClient } from "@angular/common/http";
import {
  HttpTestingController,
  provideHttpClientTesting,
} from "@angular/common/http/testing";
import { TestBed } from "@angular/core/testing";
import { MatDialog } from "@angular/material/dialog";
import { TranslateModule } from "@ngx-translate/core";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { testQueryClient } from "../../../setupJest";
import { runMutationOnSuccess } from "../../test-helpers";
import { UtilService } from "../core/service/util.service";
import { BpmnService } from "./bpmn.service";

describe(BpmnService.name, () => {
  let service: BpmnService;
  let utilService: UtilService;
  let httpTestingController: HttpTestingController;
  let dialog: MatDialog;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [TranslateModule.forRoot()],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideQueryClient(testQueryClient),
      ],
    });

    service = TestBed.inject(BpmnService);
    utilService = TestBed.inject(UtilService);
    httpTestingController = TestBed.inject(HttpTestingController);
    dialog = TestBed.inject(MatDialog);
    jest.spyOn(utilService, "openSnackbar").mockImplementation(() => {});
    jest.spyOn(dialog, "open").mockImplementation(() => ({}) as never);
  });

  afterEach(() => {
    testQueryClient.clear();
    jest.clearAllMocks();
  });

  describe("downloadProcessDefinition", () => {
    it("requests the zip of the given process definition as a blob response", () => {
      const zipBlob = new Blob(["fakeZipContent"]);
      const responses: HttpResponse<Blob>[] = [];

      service
        .downloadProcessDefinition("fakeProcessDefinitionKey")
        .subscribe((response) => responses.push(response));

      const request = httpTestingController.expectOne(
        "/rest/bpmn-process-definitions/fakeProcessDefinitionKey/download",
      );
      expect(request.request.method).toBe("GET");
      expect(request.request.responseType).toBe("blob");

      request.flush(zipBlob, {
        headers: { "Content-Disposition": 'attachment; filename="zaak.zip"' },
      });

      expect(responses[0].body).toBe(zipBlob);
      expect(responses[0].headers.get("Content-Disposition")).toBe(
        'attachment; filename="zaak.zip"',
      );
    });

    it("reports a failure to its caller instead of to the generic error dialog", () => {
      const errors: unknown[] = [];

      service
        .downloadProcessDefinition("fakeProcessDefinitionKey")
        .subscribe({ error: (error) => errors.push(error) });

      httpTestingController
        .expectOne(
          "/rest/bpmn-process-definitions/fakeProcessDefinitionKey/download",
        )
        .flush(null, { status: 500, statusText: "Server Error" });

      expect(errors).toHaveLength(1);
      expect(dialog.open).not.toHaveBeenCalled();
    });
  });

  describe("deleteProcessDefinition", () => {
    it("names the deleted process definition in the confirmation", async () => {
      await runMutationOnSuccess(
        service.deleteProcessDefinition({
          key: "fakeProcessDefinitionKey",
          name: "fakeProcessDefinitionName",
        }),
      );

      expect(utilService.openSnackbar).toHaveBeenCalledWith(
        "msg.bpmn.process-definition.deleted",
        { naam: "fakeProcessDefinitionName" },
      );
    });
  });

  describe("mutations that change the process definitions", () => {
    let invalidateQueries: jest.SpyInstance;

    beforeEach(() => {
      invalidateQueries = jest
        .spyOn(testQueryClient, "invalidateQueries")
        .mockResolvedValue(undefined);
    });

    const expectListingInvalidated = () =>
      expect(invalidateQueries).toHaveBeenCalledWith({
        queryKey: ["/rest/bpmn-process-definitions"],
      });

    it("invalidates every listing variant after an upload", async () => {
      await runMutationOnSuccess(service.uploadProcessDefinitionQuery());

      expectListingInvalidated();
    });

    it("invalidates every listing variant after deleting a definition", async () => {
      await runMutationOnSuccess(
        service.deleteProcessDefinition({
          key: "fakeProcessDefinitionKey",
          name: "fakeProcessDefinitionName",
        }),
      );

      expectListingInvalidated();
    });

    it("invalidates every listing variant after deleting a form", async () => {
      await runMutationOnSuccess(
        service.deleteProcessDefinitionForm(
          "fakeProcessDefinitionKey",
          "fakeFormName",
        ),
      );

      expectListingInvalidated();
    });
  });
});
