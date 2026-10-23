/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HttpTestingController } from "@angular/common/http/testing";
import { TestBed } from "@angular/core/testing";
import { provideTranslateService } from "@ngx-translate/core";
import { sleep, testQueryClient } from "../../../setupJest";
import { fromPartial, runMutationOnSuccess } from "../../test-helpers";
import { UtilService } from "../core/service/util.service";
import { MailtemplateService } from "../mailtemplate/mailtemplate.service";
import { runMutation } from "../shared/http/run-mutation";
import { GeneratedType } from "../shared/utils/generated-types";
import { MailtemplateKoppelingService } from "./mailtemplate-koppeling.service";
import { ZaakafhandelParametersService } from "./zaakafhandel-parameters.service";

describe(ZaakafhandelParametersService.name, () => {
  let service: ZaakafhandelParametersService;
  let utilService: UtilService;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideTranslateService()],
    });

    service = TestBed.inject(ZaakafhandelParametersService);
    utilService = TestBed.inject(UtilService);
    httpTestingController = TestBed.inject(HttpTestingController);
    jest.spyOn(utilService, "openSnackbar").mockImplementation(() => {});
  });

  describe("updateZaakafhandelparameters", () => {
    it("confirms the save to the user", async () => {
      await runMutationOnSuccess(
        service.updateZaakafhandelparameters("fakeZaaktypeUuid"),
      );

      expect(utilService.openSnackbar).toHaveBeenCalledWith(
        "msg.zaakafhandelparameters.opgeslagen",
      );
    });

    it("invalidates the mailtemplate koppelingen, which the parameters hold", async () => {
      const invalidateQueries = jest
        .spyOn(testQueryClient, "invalidateQueries")
        .mockResolvedValue(undefined);

      await runMutationOnSuccess(
        service.updateZaakafhandelparameters("fakeZaaktypeUuid"),
      );

      expect(invalidateQueries).toHaveBeenCalledWith({
        queryKey: TestBed.inject(
          MailtemplateKoppelingService,
        ).listMailtemplateKoppelingen().queryKey,
      });
    });

    it("makes zaken fetch their mailtemplate again, as the koppelingen choose it", async () => {
      const invalidateFoundMailtemplates = jest
        .spyOn(
          TestBed.inject(MailtemplateService),
          "invalidateFoundMailtemplates",
        )
        .mockResolvedValue(undefined);

      await runMutationOnSuccess(
        service.updateZaakafhandelparameters("fakeZaaktypeUuid"),
      );

      expect(invalidateFoundMailtemplates).toHaveBeenCalled();
    });

    it("puts the configuration of the zaaktype to its own resource", async () => {
      const configuration = fromPartial<
        GeneratedType<"RestZaaktypeConfiguration">
      >({
        zaaktype: { uuid: "fakeZaaktypeUuid" },
        processDefinition: {
          processEngine: "BPMN",
          key: "fakeProcessDefinitionKey",
        },
      });

      runMutation(
        testQueryClient,
        service.updateZaakafhandelparameters("fakeZaaktypeUuid"),
        configuration,
      ).subscribe();
      await sleep();

      const request = httpTestingController.expectOne(
        "/rest/zaakafhandelparameters/fakeZaaktypeUuid",
      );
      expect(request.request.method).toBe("PUT");
      expect(request.request.body).toEqual(configuration);
      request.flush(configuration);
    });

    it("makes the screen read the stored configuration again", async () => {
      const invalidateQueries = jest
        .spyOn(testQueryClient, "invalidateQueries")
        .mockResolvedValue(undefined);

      await runMutationOnSuccess(
        service.updateZaakafhandelparameters("fakeZaaktypeUuid"),
      );

      expect(invalidateQueries).toHaveBeenCalledWith({
        queryKey:
          service.readZaakafhandelparameters("fakeZaaktypeUuid").queryKey,
      });
    });
  });
});
