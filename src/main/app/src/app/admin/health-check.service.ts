/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { Observable } from "rxjs";
import { catchError } from "rxjs/operators";
import { FoutAfhandelingService } from "../fout-afhandeling/fout-afhandeling.service";
import { GeneratedType } from "../shared/utils/generated-types";
import { BuildInformatie } from "./model/build-informatie";

@Injectable({
  providedIn: "root",
})
export class HealthCheckService {
  constructor(
    private http: HttpClient,
    private foutAfhandelingService: FoutAfhandelingService,
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

  readBuildInformatie(): Observable<BuildInformatie> {
    return this.http
      .get<BuildInformatie>(`${this.basepath}/build-informatie`)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }
}
