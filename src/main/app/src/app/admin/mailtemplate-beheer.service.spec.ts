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
import { MailtemplateBeheerService } from "./mailtemplate-beheer.service";

describe(MailtemplateBeheerService.name, () => {
  let service: MailtemplateBeheerService;
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

    service = TestBed.inject(MailtemplateBeheerService);
    utilService = TestBed.inject(UtilService);
    httpTestingController = TestBed.inject(HttpTestingController);
    jest.spyOn(utilService, "openSnackbar").mockImplementation(() => {});
  });

  afterEach(() => {
    httpTestingController.verify();
    testQueryClient.clear();
    jest.clearAllMocks();
  });

  describe("deleteMailtemplate", () => {
    it("addresses the mailtemplate by its id", async () => {
      const request = service.deleteMailtemplate().mutationFn!(
        1,
        fromPartial<MutationFunctionContext>({}),
      );
      httpTestingController
        .expectOne("/rest/beheer/mailtemplates/1")
        .flush(null);

      await request;
    });

    it("confirms the deletion to the user", async () => {
      await runMutationOnSuccess(service.deleteMailtemplate(), 1);

      expect(utilService.openSnackbar).toHaveBeenCalledWith(
        "msg.mailtemplate.verwijderen.uitgevoerd",
      );
    });
  });
});
