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
import {
  type MutationFunctionContext,
  provideQueryClient,
} from "@tanstack/angular-query-experimental";
import { testQueryClient } from "../../../setupJest";
import { fromPartial, runMutationOnSuccess } from "../../test-helpers";
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
    httpTestingController.verify();
    testQueryClient.clear();
    jest.clearAllMocks();
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
