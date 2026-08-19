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
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { testQueryClient } from "../../../setupJest";
import { runMutationOnSuccess } from "../../test-helpers";
import { UtilService } from "../core/service/util.service";
import { BpmnService } from "./bpmn.service";

describe(BpmnService.name, () => {
  let service: BpmnService;
  let utilService: UtilService;
  let httpTestingController: HttpTestingController;

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
    jest.spyOn(utilService, "openSnackbar").mockImplementation(() => {});
  });

  afterEach(() => {
    testQueryClient.clear();
    jest.clearAllMocks();
  });

  describe("downloadProcessDefinition", () => {
    it("requests the zip of the given process definition as a blob", () => {
      const zipBlob = new Blob(["fakeZipContent"]);
      const responses: Blob[] = [];

      service
        .downloadProcessDefinition("fakeProcessDefinitionKey")
        .subscribe((response) => responses.push(response));

      const request = httpTestingController.expectOne(
        "/rest/bpmn-process-definitions/fakeProcessDefinitionKey/download",
      );
      expect(request.request.method).toBe("GET");
      expect(request.request.responseType).toBe("blob");

      request.flush(zipBlob);

      expect(responses).toEqual([zipBlob]);
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
