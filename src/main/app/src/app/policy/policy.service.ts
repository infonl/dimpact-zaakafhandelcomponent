/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { Observable } from "rxjs";
import { catchError } from "rxjs/operators";
import { FoutAfhandelingService } from "../fout-afhandeling/fout-afhandeling.service";
import { OverigeRechten } from "./model/overige-rechten";
import { WerklijstRechten } from "./model/werklijst-rechten";
import { ZacHttpClient } from "../shared/http/zac-http-client";

@Injectable({
  providedIn: "root",
})
export class PolicyService {
  constructor(
    private http: HttpClient,
    private zacHttp: ZacHttpClient,
    private foutAfhandelingService: FoutAfhandelingService,
  ) {}

  private basepath = "/rest/policy";

  readWerklijstRechten() {
    return this.zacHttp
      .GET(`/rest/policy/werklijstRechten`)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  readOverigeRechten() {
    return this.zacHttp
      .GET(`/rest/policy/overigeRechten`)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  readNotitieRechten() {
    return this.zacHttp
      .GET(`/rest/policy/notitieRechten`)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }
}
