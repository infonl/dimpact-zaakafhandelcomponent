/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { inject, Injectable } from "@angular/core";
import { StaleTimes, ZacQueryClient } from "../shared/http/zac-query-client";

@Injectable({
  providedIn: "root",
})
export class HealthCheckService {
  private readonly zacQueryClient = inject(ZacQueryClient);

  listZaaktypeInrichtingschecks() {
    return {
      ...this.zacQueryClient.GET("/rest/health-check/zaaktypes"),
      staleTime: StaleTimes.Instant,
    };
  }

  readBestaatCommunicatiekanaalEformulier() {
    return {
      ...this.zacQueryClient.GET(
        "/rest/health-check/bestaat-communicatiekanaal-eformulier",
      ),
      staleTime: StaleTimes.Instant,
    };
  }

  readBuildInformatie() {
    return this.zacQueryClient.GET("/rest/health-check/build-informatie");
  }
}
