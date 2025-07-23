/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Injectable } from "@angular/core";
import { PutBody, ZacHttpClient } from "../shared/http/zac-http-client";

@Injectable({
  providedIn: "root",
})
export class ContactmomentenService {
  constructor(private readonly zacHttpClient: ZacHttpClient) {}

  listContactmomenten(body: PutBody<"/rest/klanten/contactmomenten">) {
    return this.zacHttpClient.PUT("/rest/klanten/contactmomenten", body);
  }
}
