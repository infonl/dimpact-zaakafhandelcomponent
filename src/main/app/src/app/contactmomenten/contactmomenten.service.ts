/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { inject, Injectable } from "@angular/core";
import { PutBody } from "../shared/http/http-client";
import { ZacQueryClient } from "../shared/http/zac-query-client";

@Injectable({
  providedIn: "root",
})
export class ContactmomentenService {
  private readonly zacQueryClient = inject(ZacQueryClient);

  listContactmomenten(body: PutBody<"/rest/klanten/contactmomenten">) {
    return this.zacQueryClient.PUT_QUERY("/rest/klanten/contactmomenten", body);
  }
}
