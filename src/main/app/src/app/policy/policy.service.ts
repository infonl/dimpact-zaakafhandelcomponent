/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Injectable } from "@angular/core";
import { ZacHttpClient } from "../shared/http/zac-http-client";

@Injectable({
  providedIn: "root",
})
export class PolicyService {
  constructor(private readonly zacHttpClient: ZacHttpClient) {}

  readWerklijstRechten() {
    return this.zacHttpClient.GET(`/rest/policy/werklijstRechten`, {});
  }

  readOverigeRechten() {
    return this.zacHttpClient.GET(`/rest/policy/overigeRechten`, {});
  }

  readNotitieRechten() {
    return this.zacHttpClient.GET(`/rest/policy/notitieRechten`, {});
  }
}
