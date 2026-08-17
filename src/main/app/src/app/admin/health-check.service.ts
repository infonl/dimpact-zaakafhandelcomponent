/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { inject, Injectable } from "@angular/core";
import { Observable } from "rxjs";
import { ZacHttpClient } from "../shared/http/zac-http-client";
import { ZacQueryClient } from "../shared/http/zac-query-client";

@Injectable({
  providedIn: "root",
})
export class HealthCheckService {
  private readonly zacHttpClient = inject(ZacHttpClient);
  private readonly zacQueryClient = inject(ZacQueryClient);

  listZaaktypeInrichtingschecks() {
    return this.zacHttpClient.GET("/rest/health-check/zaaktypes");
  }

  readBestaatCommunicatiekanaalEformulier() {
    return this.zacHttpClient.GET(
      "/rest/health-check/bestaat-communicatiekanaal-eformulier",
    );
  }

  clearZTCCaches() {
    return this.zacQueryClient.DELETE("/rest/health-check/ztc-cache");
  }

  readZTCCacheTime(): Observable<string> {
    return this.zacHttpClient.GET("/rest/health-check/ztc-cache");
  }

  readBuildInformatie() {
    return this.zacHttpClient.GET("/rest/health-check/build-informatie");
  }
}
