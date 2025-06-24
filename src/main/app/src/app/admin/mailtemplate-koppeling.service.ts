/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Injectable } from "@angular/core";
import { ZacHttpClient } from "../shared/http/zac-http-client";

@Injectable({
  providedIn: "root",
})
export class MailtemplateKoppelingService {
  constructor(private readonly zacHttpClient: ZacHttpClient) {}

  listMailtemplateKoppelingen() {
    return this.zacHttpClient.GET("/rest/beheer/mailtemplatekoppeling", {});
  }
}
