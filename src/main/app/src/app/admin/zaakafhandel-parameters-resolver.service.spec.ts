/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { TestBed } from "@angular/core/testing";
import { ActivatedRouteSnapshot, convertToParamMap } from "@angular/router";
import { provideTranslateService } from "@ngx-translate/core";
import { fromPartial } from "src/test-helpers";
import { testQueryClient } from "../../../setupJest";
import { GeneratedType } from "../shared/utils/generated-types";
import { ZaakafhandelParametersResolver } from "./zaakafhandel-parameters-resolver.service";
import { ZaakafhandelParametersService } from "./zaakafhandel-parameters.service";

describe(ZaakafhandelParametersResolver.name, () => {
  const configuration = fromPartial<GeneratedType<"RestZaaktypeConfiguration">>(
    {
      zaaktype: { uuid: "fakeZaaktypeUuid" },
      processDefinition: {
        processEngine: "BPMN",
        key: "fakeProcessDefinitionKey",
      },
    },
  );

  let resolver: ZaakafhandelParametersResolver;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideTranslateService()] });
    resolver = TestBed.inject(ZaakafhandelParametersResolver);
    jest.spyOn(testQueryClient, "fetchQuery").mockResolvedValue(configuration);
  });

  function routeWith(uuid: string | null) {
    return fromPartial<ActivatedRouteSnapshot>({
      get paramMap() {
        return convertToParamMap({ uuid });
      },
    });
  }

  it("refuses a route without a zaaktype uuid", () => {
    expect(() => resolver.resolve(routeWith(null))).toThrow(
      "ZaakafhandelParametersResolver: no 'uuid' parameter found in route",
    );
  });

  it("resolves the configuration of the zaaktype with one fresh read", async () => {
    const result = await resolver.resolve(routeWith("fakeZaaktypeUuid"));

    expect(result).toBe(configuration);
    expect(testQueryClient.fetchQuery).toHaveBeenCalledTimes(1);
    expect(testQueryClient.fetchQuery).toHaveBeenCalledWith(
      expect.objectContaining({
        queryKey: TestBed.inject(
          ZaakafhandelParametersService,
        ).readZaakafhandelparameters("fakeZaaktypeUuid").queryKey,
        staleTime: 0,
      }),
    );
  });
});
