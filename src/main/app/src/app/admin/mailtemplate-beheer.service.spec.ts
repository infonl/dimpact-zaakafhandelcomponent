/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { provideHttpClient } from "@angular/common/http";
import { TestBed } from "@angular/core/testing";
import { TranslateModule } from "@ngx-translate/core";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { testQueryClient } from "../../../setupJest";
import { runMutationOnSuccess } from "../../test-helpers";
import { UtilService } from "../core/service/util.service";
import { MailtemplateBeheerService } from "./mailtemplate-beheer.service";

describe(MailtemplateBeheerService.name, () => {
  let service: MailtemplateBeheerService;
  let utilService: UtilService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [TranslateModule.forRoot()],
      providers: [provideHttpClient(), provideQueryClient(testQueryClient)],
    });

    service = TestBed.inject(MailtemplateBeheerService);
    utilService = TestBed.inject(UtilService);
    jest.spyOn(utilService, "openSnackbar").mockImplementation(() => {});
  });

  afterEach(() => {
    testQueryClient.clear();
    jest.clearAllMocks();
  });

  describe("deleteMailtemplate", () => {
    it("confirms the deletion to the user", async () => {
      await runMutationOnSuccess(service.deleteMailtemplate(), 1);

      expect(utilService.openSnackbar).toHaveBeenCalledWith(
        "msg.mailtemplate.verwijderen.uitgevoerd",
      );
    });
  });
});
