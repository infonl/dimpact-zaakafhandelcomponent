/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { inject, Injectable } from "@angular/core";
import { ZacHttpClient } from "../shared/http/zac-http-client";
import { ZacQueryClient } from "../shared/http/zac-query-client";

@Injectable({
  providedIn: "root",
})
export class SignaleringenSettingsBeheerService {
  private readonly zacHttpClient = inject(ZacHttpClient);
  private readonly zacQueryClient = inject(ZacQueryClient);

  list(groupId: string) {
    return this.zacHttpClient.GET(
      "/rest/signaleringen/group/{groupId}/instellingen",
      { path: { groupId } },
    );
  }

  put(groupId: string) {
    return this.zacQueryClient.PUT(
      "/rest/signaleringen/group/{groupId}/instellingen",
      { path: { groupId } },
    );
  }
}
