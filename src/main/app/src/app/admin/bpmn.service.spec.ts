/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HttpTestingController } from "@angular/common/http/testing";
import { TestBed } from "@angular/core/testing";
import { MatDialog } from "@angular/material/dialog";
import { provideTranslateService } from "@ngx-translate/core";
import { type MutationFunctionContext } from "@tanstack/angular-query-experimental";
import { sleep, testQueryClient } from "../../../setupJest";
import { fromPartial, runMutationOnSuccess } from "../../test-helpers";
import { UtilService } from "../core/service/util.service";
import { runMutation } from "../shared/http/run-mutation";
import { BpmnService } from "./bpmn.service";

describe(BpmnService.name, () => {
  let service: BpmnService;
  let utilService: UtilService;
  let httpTestingController: HttpTestingController;
  let dialog: MatDialog;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideTranslateService()],
    });

    service = TestBed.inject(BpmnService);
    utilService = TestBed.inject(UtilService);
    httpTestingController = TestBed.inject(HttpTestingController);
    dialog = TestBed.inject(MatDialog);
    jest.spyOn(utilService, "openSnackbar").mockImplementation(() => {});
    jest.spyOn(dialog, "open").mockImplementation(() => ({}) as never);
  });

  describe("downloadProcessDefinition", () => {
    const DOWNLOAD_URL =
      "/rest/bpmn-process-definitions/fakeProcessDefinitionKey/download";

    const download = () =>
      testQueryClient.fetchQuery(
        service.downloadProcessDefinition("fakeProcessDefinitionKey"),
      );

    it("requests the zip of the given process definition as a blob response", async () => {
      const zipBlob = new Blob(["fakeZipContent"]);

      const response = download();
      await sleep();

      const request = httpTestingController.expectOne(DOWNLOAD_URL);
      expect(request.request.method).toBe("GET");
      expect(request.request.responseType).toBe("blob");

      request.flush(zipBlob, {
        headers: { "Content-Disposition": 'attachment; filename="zaak.zip"' },
      });

      const { body, headers } = await response;
      expect(body).toBe(zipBlob);
      expect(headers.get("Content-Disposition")).toBe(
        'attachment; filename="zaak.zip"',
      );
    });

    it("requests the zip again on every download instead of serving it from the cache", async () => {
      const firstDownload = download();
      await sleep();
      httpTestingController.expectOne(DOWNLOAD_URL).flush(new Blob());
      await firstDownload;

      const secondDownload = download();
      await sleep();
      httpTestingController.expectOne(DOWNLOAD_URL).flush(new Blob());
      await secondDownload;
    });

    it("reports a failure to its caller instead of to the generic error dialog", async () => {
      const response = download();
      await sleep();

      httpTestingController
        .expectOne(DOWNLOAD_URL)
        .flush(null, { status: 500, statusText: "Server Error" });

      await expect(response).rejects.toMatchObject({ status: 500 });
      expect(dialog.open).not.toHaveBeenCalled();
    });
  });

  describe("deleteProcessDefinition", () => {
    it("addresses the process definition by its key", async () => {
      const request = service.deleteProcessDefinition().mutationFn!(
        { key: "fakeProcessDefinitionKey", name: "fakeProcessDefinitionName" },
        fromPartial<MutationFunctionContext>({}),
      );
      httpTestingController
        .expectOne("/rest/bpmn-process-definitions/fakeProcessDefinitionKey")
        .flush(null);

      await request;
    });

    it("addresses a form by its process definition key and name", async () => {
      const request = service.deleteProcessDefinitionForm().mutationFn!(
        {
          processDefinitionKey: "fakeProcessDefinitionKey",
          name: "fakeFormName",
        },
        fromPartial<MutationFunctionContext>({}),
      );
      httpTestingController
        .expectOne(
          "/rest/bpmn-process-definitions/fakeProcessDefinitionKey/forms/fakeFormName",
        )
        .flush(null);

      await request;
    });

    it("names the deleted process definition in the confirmation", async () => {
      await runMutationOnSuccess(service.deleteProcessDefinition(), {
        key: "fakeProcessDefinitionKey",
        name: "fakeProcessDefinitionName",
      });

      expect(utilService.openSnackbar).toHaveBeenCalledWith(
        "msg.bpmn.process-definition.deleted",
        { naam: "fakeProcessDefinitionName" },
      );
    });
  });

  describe("uploadProcessDefinitionForm", () => {
    it("posts the form to the process definition it belongs to", async () => {
      const form = { filename: "fakeFormName.json", content: '{"form": true}' };

      runMutation(
        testQueryClient,
        service.uploadProcessDefinitionForm("fakeProcessDefinitionKey"),
        form,
      ).subscribe();
      await sleep();

      const request = httpTestingController.expectOne(
        "/rest/bpmn-process-definitions/fakeProcessDefinitionKey/forms",
      );
      expect(request.request.method).toBe("POST");
      expect(request.request.body).toEqual(form);
      request.flush(null);
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
      await runMutationOnSuccess(service.deleteProcessDefinition(), {
        key: "fakeProcessDefinitionKey",
        name: "fakeProcessDefinitionName",
      });

      expectListingInvalidated();
    });

    it("invalidates every listing variant after deleting a form", async () => {
      await runMutationOnSuccess(service.deleteProcessDefinitionForm(), {
        processDefinitionKey: "fakeProcessDefinitionKey",
        name: "fakeFormName",
      });

      expectListingInvalidated();
    });
  });
});
