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

  describe("saveMailtemplate", () => {
    it("posts a template that has no id yet", () => {
      expect(service.saveMailtemplate(null).mutationKey).toEqual([
        "/rest/beheer/mailtemplates",
      ]);
    });

    it("puts a template that already has an id", () => {
      expect(service.saveMailtemplate(1).mutationKey).toEqual([
        "/rest/beheer/mailtemplates/{id}",
        { path: { id: 1 } },
      ]);
    });

    it("confirms the save to the user", async () => {
      await runMutationOnSuccess(service.saveMailtemplate(1));

      expect(utilService.openSnackbar).toHaveBeenCalledWith(
        "msg.mailtemplate.opgeslagen",
      );
    });

    it("invalidates the template it updated", async () => {
      const invalidateQueries = jest
        .spyOn(testQueryClient, "invalidateQueries")
        .mockResolvedValue(undefined);

      await runMutationOnSuccess(service.saveMailtemplate(1));

      expect(invalidateQueries).toHaveBeenCalledWith({
        queryKey: service.readMailtemplateQuery(1).queryKey,
      });
    });

    it("has nothing to invalidate for a template it created", async () => {
      const invalidateQueries = jest
        .spyOn(testQueryClient, "invalidateQueries")
        .mockResolvedValue(undefined);

      await runMutationOnSuccess(service.saveMailtemplate(null));

      expect(invalidateQueries).not.toHaveBeenCalled();
    });
  });

  describe("deleteMailtemplate", () => {
    it("confirms the deletion to the user", async () => {
      await runMutationOnSuccess(service.deleteMailtemplate(1));

      expect(utilService.openSnackbar).toHaveBeenCalledWith(
        "msg.mailtemplate.verwijderen.uitgevoerd",
      );
    });
  });
});
