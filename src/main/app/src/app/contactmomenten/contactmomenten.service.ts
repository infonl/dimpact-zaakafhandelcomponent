/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { inject, Injectable } from "@angular/core";
import { ZacQueryClient } from "../shared/http/zac-query-client";

@Injectable({
  providedIn: "root",
})
export class ContactmomentenService {
  private readonly zacQueryClient = inject(ZacQueryClient);

  listContactmomenten() {
    return this.zacQueryClient.PUT("/rest/klanten/contactmomenten");
  }
}
