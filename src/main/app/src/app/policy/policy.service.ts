/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { inject, Injectable } from "@angular/core";
import { ZacQueryClient } from "../shared/http/zac-query-client";

@Injectable({
  providedIn: "root",
})
export class PolicyService {
  private readonly zacQueryClient = inject(ZacQueryClient);

  readWerklijstRechten() {
    return this.zacQueryClient.GET(`/rest/policy/werklijstRechten`);
  }

  readOverigeRechten() {
    return this.zacQueryClient.GET(`/rest/policy/overigeRechten`);
  }

  readNotitieRechten() {
    return this.zacQueryClient.GET(`/rest/policy/notitieRechten`);
  }

  readBrpRechten() {
    return this.zacQueryClient.GET(`/rest/policy/brpRechten`);
  }
}
