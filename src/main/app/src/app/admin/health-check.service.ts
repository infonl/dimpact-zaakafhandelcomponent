/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { inject, Injectable } from "@angular/core";
import { queryOptions } from "@tanstack/angular-query-experimental";
import { PathParameters } from "../shared/http/http-client";
import { StaleTimes, ZacQueryClient } from "../shared/http/zac-query-client";

export type InrichtingscheckParameters = NonNullable<
  NonNullable<PathParameters<"/rest/health-check/zaaktypes", "get">>["query"]
>;

@Injectable({
  providedIn: "root",
})
export class HealthCheckService {
  private readonly zacQueryClient = inject(ZacQueryClient);

  listZaaktypeInrichtingschecks(parameters: InrichtingscheckParameters) {
    return queryOptions({
      ...this.zacQueryClient.GET("/rest/health-check/zaaktypes", {
        query: parameters,
      }),
      staleTime: StaleTimes.Instant,
    });
  }

  readReferentietabellenInrichtingscheck() {
    return queryOptions({
      ...this.zacQueryClient.GET("/rest/health-check/referentietabellen"),
      staleTime: StaleTimes.Instant,
    });
  }

  readBuildInformatie() {
    return this.zacQueryClient.GET("/rest/health-check/build-informatie");
  }
}
