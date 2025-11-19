/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Injectable } from "@angular/core";
import { Observable } from "rxjs";
import { ZacHttpClient } from "../shared/http/zac-http-client";

@Injectable({
  providedIn: "root",
})
export class HealthCheckService {
  constructor(private readonly zacHttpClient: ZacHttpClient) {}

  listZaaktypeInrichtingschecks() {
    return this.zacHttpClient.GET("/rest/health-check/zaaktypes");
  }

  readBestaatCommunicatiekanaalEformulier() {
    return this.zacHttpClient.GET(
      "/rest/health-check/bestaat-communicatiekanaal-eformulier",
    );
  }

  clearZTCCaches() {
    return this.zacHttpClient.DELETE("/rest/health-check/ztc-cache");
  }

  readZTCCacheTime(): Observable<string> {
    return this.zacHttpClient.GET("/rest/health-check/ztc-cache");
  }

  readBuildInformatie() {
    return this.zacHttpClient.GET("/rest/health-check/build-informatie");
  }
}
