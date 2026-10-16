/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { inject, Injectable } from "@angular/core";
import { ZacHttpClient } from "../shared/http/zac-http-client";

@Injectable({
  providedIn: "root",
})
export class HealthCheckService {
  private readonly zacHttpClient = inject(ZacHttpClient);

  listZaaktypeInrichtingschecks() {
    return this.zacHttpClient.GET("/rest/health-check/zaaktypes");
  }

  readBestaatCommunicatiekanaalEformulier() {
    return this.zacHttpClient.GET(
      "/rest/health-check/bestaat-communicatiekanaal-eformulier",
    );
  }

  readBuildInformatie() {
    return this.zacHttpClient.GET("/rest/health-check/build-informatie");
  }
}
