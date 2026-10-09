/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { inject, Injectable } from "@angular/core";
import { ZacQueryClient } from "../shared/http/zac-query-client";

@Injectable({
  providedIn: "root",
})
export class MailtemplateKoppelingService {
  private readonly zacQueryClient = inject(ZacQueryClient);

  listMailtemplateKoppelingen() {
    return this.zacQueryClient.GET("/rest/beheer/mailtemplatekoppeling");
  }
}
