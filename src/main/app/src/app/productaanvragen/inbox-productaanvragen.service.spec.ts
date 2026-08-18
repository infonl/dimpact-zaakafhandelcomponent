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
import { InboxProductaanvragenService } from "./inbox-productaanvragen.service";

describe(InboxProductaanvragenService.name, () => {
  let service: InboxProductaanvragenService;
  let utilService: UtilService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [TranslateModule.forRoot()],
      providers: [provideHttpClient(), provideQueryClient(testQueryClient)],
    });

    service = TestBed.inject(InboxProductaanvragenService);
    utilService = TestBed.inject(UtilService);
    jest.spyOn(utilService, "openSnackbar").mockImplementation(() => {});
  });

  describe("delete", () => {
    it("confirms the deletion to the user", async () => {
      await runMutationOnSuccess(service.delete(42));

      expect(utilService.openSnackbar).toHaveBeenCalledWith(
        "msg.inboxProductaanvraag.verwijderen.uitgevoerd",
      );
    });
  });
});
