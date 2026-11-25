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

  afterEach(() => {
    jest.restoreAllMocks();
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

    it("makes every page of the overview of all zaaktypes read the parameters again, but not the configuration of another zaaktype", async () => {
      const firstPage = service.listZaakafhandelParameters({
        page: 0,
        rows: 25,
      }).queryKey;
      const secondPage = service.listZaakafhandelParameters({
        page: 1,
        rows: 25,
        isValide: true,
      }).queryKey;
      const otherConfiguration = service.readZaakafhandelparameters(
        "fakeOtherZaaktypeUuid",
      ).queryKey;
      const processDefinitions = service.listProcessDefinitions().queryKey;
      for (const queryKey of [
        firstPage,
        secondPage,
        otherConfiguration,
        processDefinitions,
      ]) {
        testQueryClient.setQueryData(queryKey, fromPartial({}));
      }

      await runMutationOnSuccess(
        service.updateZaakafhandelparameters("fakeZaaktypeUuid"),
      );

      const isInvalidated = (queryKey: readonly unknown[]) =>
        testQueryClient.getQueryState(queryKey)?.isInvalidated;
      expect(isInvalidated(firstPage)).toBe(true);
      expect(isInvalidated(secondPage)).toBe(true);
      expect(isInvalidated(otherConfiguration)).toBe(false);
      expect(isInvalidated(processDefinitions)).toBe(false);
    });

    it("makes the afbreken dialog read the zaakbeeindigredenen of the zaaktype again, as the parameters hold them", async () => {
      const invalidateQueries = jest
        .spyOn(testQueryClient, "invalidateQueries")
        .mockResolvedValue(undefined);

      await runMutationOnSuccess(
        service.updateZaakafhandelparameters("fakeZaaktypeUuid"),
      );

      expect(invalidateQueries).toHaveBeenCalledWith({
        queryKey:
          service.listZaakbeeindigRedenenForZaaktype("fakeZaaktypeUuid")
            .queryKey,
      });
    });
  });

  describe("listZaakafhandelParameters", () => {
    it("reads one page of the parameters of the zaaktypes with the filters as query parameters", async () => {
      const page = fromPartial<
        GeneratedType<"RestZaaktypeConfigurationOverzichtResultaat">
      >({ totaal: 26, resultaten: [] });

      const result = testQueryClient.fetchQuery(
        service.listZaakafhandelParameters({
          page: 1,
          rows: 25,
          isValide: true,
          zaaktypeIdentificatie: null,
        }),
      );
      await sleep();
      const request = httpTestingController.expectOne(
        "/rest/zaakafhandelparameters?page=1&rows=25&isValide=true",
      );
      expect(request.request.method).toBe("GET");
      request.flush(page);

      await expect(result).resolves.toEqual(page);
    });
  });

  describe("listZaakbeeindigRedenenForZaaktype", () => {
    it("reads the zaakbeeindigredenen of the zaaktype", async () => {
      const redenen = [
        fromPartial<GeneratedType<"RestZaakbeeindigReden">>({
          id: "fakeRedenId",
        }),
      ];

      const result = testQueryClient.fetchQuery(
        service.listZaakbeeindigRedenenForZaaktype("fakeZaaktypeUuid"),
      );
      await sleep();
      httpTestingController
        .expectOne(
          "/rest/zaakafhandelparameters/zaakbeeindigredenen/fakeZaaktypeUuid",
        )
        .flush(redenen);

      await expect(result).resolves.toEqual(redenen);
    });
  });
});
