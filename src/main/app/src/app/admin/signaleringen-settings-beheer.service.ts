/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Injectable } from "@angular/core";
import { PutBody, ZacHttpClient } from "../shared/http/zac-http-client";

@Injectable({
  providedIn: "root",
})
export class SignaleringenSettingsBeheerService {
  constructor(private readonly zacHttpClient: ZacHttpClient) {}

  list(groupId: string) {
    return this.zacHttpClient.GET(
      "/rest/signaleringen/group/{groupId}/instellingen",
      { path: { groupId } },
    );
  }

  put(
    groupId: string,
    body: PutBody<"/rest/signaleringen/group/{groupId}/instellingen">,
  ) {
    return this.zacHttpClient.PUT(
      "/rest/signaleringen/group/{groupId}/instellingen",
      body,
      { path: { groupId } },
    );
  }
}
