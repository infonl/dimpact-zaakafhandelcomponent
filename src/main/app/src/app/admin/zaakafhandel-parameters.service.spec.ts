/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HttpTestingController } from "@angular/common/http/testing";
import { TestBed } from "@angular/core/testing";
import { TranslateModule } from "@ngx-translate/core";
import { sleep, testQueryClient } from "../../../setupJest";
import { fromPartial, runMutationOnSuccess } from "../../test-helpers";
import { UtilService } from "../core/service/util.service";
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
      imports: [TranslateModule.forRoot()],
      providers: [],
    });

    service = TestBed.inject(ZaakafhandelParametersService);
    utilService = TestBed.inject(UtilService);
    httpTestingController = TestBed.inject(HttpTestingController);
    jest.spyOn(utilService, "openSnackbar").mockImplementation(() => {});
  });

  describe("updateZaakafhandelparameters", () => {
    it("confirms the save to the user", async () => {
      await runMutationOnSuccess(service.updateZaakafhandelparameters());

      expect(utilService.openSnackbar).toHaveBeenCalledWith(
        "msg.zaakafhandelparameters.opgeslagen",
      );
    });

    it("invalidates the mailtemplate koppelingen, which the parameters hold", async () => {
      const invalidateQueries = jest
        .spyOn(testQueryClient, "invalidateQueries")
        .mockResolvedValue(undefined);

      await runMutationOnSuccess(service.updateZaakafhandelparameters());

      expect(invalidateQueries).toHaveBeenCalledWith({
        queryKey: TestBed.inject(
          MailtemplateKoppelingService,
        ).listMailtemplateKoppelingen().queryKey,
      });
    });
  });

  describe("createOrUpdateBpmnZaakafhandelparameters", () => {
    it("posts the BPMN configuration of the zaaktype", async () => {
      const configuration = fromPartial<
        GeneratedType<"RestZaaktypeBpmnConfiguration">
      >({
        zaaktypeUuid: "fakeZaaktypeUuid",
        bpmnProcessDefinitionKey: "fakeProcessDefinitionKey",
      });

      runMutation(
        testQueryClient,
        service.createOrUpdateBpmnZaakafhandelparameters(),
        configuration,
      ).subscribe();
      await sleep();

      const request = httpTestingController.expectOne(
        "/rest/zaaktype-bpmn-configuration",
      );
      expect(request.request.method).toBe("POST");
      expect(request.request.body).toEqual(configuration);
      request.flush(configuration);
    });

    it("confirms the save to the user", async () => {
      await runMutationOnSuccess(
        service.createOrUpdateBpmnZaakafhandelparameters(),
      );

      expect(utilService.openSnackbar).toHaveBeenCalledWith(
        "msg.zaakafhandelparameters.opgeslagen",
      );
    });
  });
});
