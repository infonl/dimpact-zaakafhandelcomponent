/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { FoutAfhandelingService } from "../fout-afhandeling/fout-afhandeling.service";
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
    return this.zacHttp.GET(`/rest/policy/werklijstRechten`);
  }

  readOverigeRechten() {
    return this.zacHttp.GET(`/rest/policy/overigeRechten`);
  }

  readNotitieRechten() {
    return this.zacHttp.GET(`/rest/policy/notitieRechten`);
  }
}
