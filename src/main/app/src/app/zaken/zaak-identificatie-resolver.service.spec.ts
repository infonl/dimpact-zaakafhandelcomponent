/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  provideHttpClient,
  withInterceptorsFromDi,
  withXhr,
} from "@angular/common/http";
import {
  HttpTestingController,
  provideHttpClientTesting,
} from "@angular/common/http/testing";
import { TestBed } from "@angular/core/testing";
import { ActivatedRouteSnapshot, convertToParamMap } from "@angular/router";
import { provideTranslateService } from "@ngx-translate/core";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { fromPartial } from "src/test-helpers";
import { sleep, testQueryClient } from "../../../setupJest";
import { GeneratedType } from "../shared/utils/generated-types";
import { ZaakIdentificatieResolver } from "./zaak-identificatie-resolver.service";

describe(ZaakIdentificatieResolver.name, () => {
  let resolver: ZaakIdentificatieResolver;
  let httpTestingController: HttpTestingController;

  const routeWith = (params: Record<string, string>) =>
    fromPartial<ActivatedRouteSnapshot>({
      paramMap: convertToParamMap(params),
    });

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideTranslateService(),
        provideHttpClient(withXhr(), withInterceptorsFromDi()),
        provideHttpClientTesting(),
        provideQueryClient(testQueryClient),
      ],
    });

    resolver = TestBed.inject(ZaakIdentificatieResolver);
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  it("resolves the zaak that the server returns for the zaakIdentificatie in the route", async () => {
    const zaak = fromPartial<GeneratedType<"RestZaak">>({
      uuid: "fakeZaakUuid",
      identificatie: "ZAAK-2026-0000000001",
    });

    const resolved = resolver.resolve(
      routeWith({ zaakIdentificatie: "ZAAK-2026-0000000001" }),
    );
    await sleep();
    httpTestingController
      .expectOne("/rest/zaken/zaak/id/ZAAK-2026-0000000001")
      .flush(zaak);

    await expect(resolved).resolves.toEqual(zaak);
  });

  it("refuses a route without a zaakIdentificatie", () => {
    expect(() => resolver.resolve(routeWith({}))).toThrow(
      "ZaakIdentificatieResolver: No 'zaakID' found in route",
    );
  });
});
