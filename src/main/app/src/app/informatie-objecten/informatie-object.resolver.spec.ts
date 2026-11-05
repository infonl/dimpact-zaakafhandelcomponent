/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HttpTestingController } from "@angular/common/http/testing";
import { TestBed } from "@angular/core/testing";
import { ActivatedRouteSnapshot, convertToParamMap } from "@angular/router";
import { provideTranslateService } from "@ngx-translate/core";
import { fromPartial } from "src/test-helpers";
import { sleep, testQueryClient } from "../../../setupJest";
import { GeneratedType } from "../shared/utils/generated-types";
import { InformatieObjectResolver } from "./informatie-object.resolver";
import { InformatieObjectenService } from "./informatie-objecten.service";

describe(InformatieObjectResolver.name, () => {
  const documentUuid = "document-uuid-1";
  const documentUrl = `/rest/informatieobjecten/informatieobject/${documentUuid}`;

  const cachedCopy = fromPartial<
    GeneratedType<"RestEnkelvoudigInformatieobject">
  >({ uuid: documentUuid, titel: "cached titel" });
  const freshCopy = fromPartial<
    GeneratedType<"RestEnkelvoudigInformatieobject">
  >({ uuid: documentUuid, titel: "fresh titel" });

  let resolver: InformatieObjectResolver;
  let service: InformatieObjectenService;
  let httpTestingController: HttpTestingController;

  function route(params: Record<string, string>) {
    return fromPartial<ActivatedRouteSnapshot>({
      paramMap: convertToParamMap(params),
    });
  }

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideTranslateService()],
    });

    resolver = TestBed.inject(InformatieObjectResolver);
    service = TestBed.inject(InformatieObjectenService);
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  it("refuses a route without a uuid", () => {
    expect(() => resolver.resolve(route({}))).toThrow(
      "InformatieObjectResolver: 'uuid' is not defined in route",
    );
  });

  it("reads the document from the server again when it is opened, because the view only hears of changes while it is open", async () => {
    testQueryClient.setQueryData(
      service.readEnkelvoudigInformatieobject(documentUuid).queryKey,
      cachedCopy,
    );

    const resolved = resolver.resolve(route({ uuid: documentUuid }));
    await sleep();
    httpTestingController.expectOne(documentUrl).flush(freshCopy);

    await expect(resolved).resolves.toEqual(freshCopy);
  });

  it("reads the opened version from the server again as well", async () => {
    const versionUrl = `/rest/informatieobjecten/informatieobject/versie/${documentUuid}/2`;
    testQueryClient.setQueryData(
      service.readEnkelvoudigInformatieobjectVersie(documentUuid, 2).queryKey,
      cachedCopy,
    );

    const resolved = resolver.resolve(
      route({ uuid: documentUuid, versie: "2" }),
    );
    await sleep();
    httpTestingController.expectOne(versionUrl).flush(freshCopy);

    await expect(resolved).resolves.toEqual(freshCopy);
  });
});
