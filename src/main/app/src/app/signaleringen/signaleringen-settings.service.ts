/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Injectable } from "@angular/core";
import { PutBody, ZacHttpClient } from "../shared/http/zac-http-client";

@Injectable({
  providedIn: "root",
})
export class SignaleringenSettingsService {
  private basepath = "/rest/signaleringen";

  constructor(private readonly zacHttpClient: ZacHttpClient) {}

  list() {
    return this.zacHttpClient.GET("/rest/signaleringen/instellingen");
  }

  put(body: PutBody<"/rest/signaleringen/instellingen">) {
    return this.zacHttpClient.PUT("/rest/signaleringen/instellingen", body);
  }
}
