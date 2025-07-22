/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { Observable } from "rxjs";
import { catchError } from "rxjs/operators";
import { FoutAfhandelingService } from "../fout-afhandeling/fout-afhandeling.service";
import { ZacHttpClient } from "../shared/http/zac-http-client";
import { GeneratedType } from "../shared/utils/generated-types";

@Injectable({
  providedIn: "root",
})
export class HealthCheckService {
  constructor(
    private readonly http: HttpClient,
    private readonly foutAfhandelingService: FoutAfhandelingService,
    private readonly zacHttpClient: ZacHttpClient,
  ) {}

  private basepath = "/rest/health-check";

  listZaaktypeInrichtingschecks(): Observable<
    GeneratedType<"RESTZaaktypeInrichtingscheck">[]
  > {
    return this.http
      .get<
        GeneratedType<"RESTZaaktypeInrichtingscheck">[]
      >(`${this.basepath}/zaaktypes`)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  readBestaatCommunicatiekanaalEformulier(): Observable<boolean> {
    return this.http
      .get<boolean>(`${this.basepath}/bestaat-communicatiekanaal-eformulier`)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  clearZTCCaches(): Observable<string> {
    return this.http
      .delete<string>(`${this.basepath}/ztc-cache`)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  readZTCCacheTime(): Observable<string> {
    return this.http
      .get<string>(`${this.basepath}/ztc-cache`)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  readBuildInformatie() {
    return this.zacHttpClient.GET("/rest/health-check/build-informatie");
  }
}
