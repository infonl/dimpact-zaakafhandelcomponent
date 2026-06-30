/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Injectable } from "@angular/core";
import { ZacHttpClient } from "../shared/http/zac-http-client";
import { ZacQueryClient } from "../shared/http/zac-query-client";

@Injectable({
  providedIn: "root",
})
export class PolicyService {
  constructor(
    private readonly zacHttpClient: ZacHttpClient,
    private readonly zacQueryClient: ZacQueryClient,
  ) {}

  readWerklijstRechten() {
    return this.zacHttpClient.GET(`/rest/policy/werklijstRechten`);
  }

  readOverigeRechten() {
    return this.zacQueryClient.GET(`/rest/policy/overigeRechten`);
  }

  readNotitieRechten() {
    return this.zacHttpClient.GET(`/rest/policy/notitieRechten`);
  }

  readBrpRechten() {
    return this.zacQueryClient.GET(`/rest/policy/brpRechten`);
  }
}
