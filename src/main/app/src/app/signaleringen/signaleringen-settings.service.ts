/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { inject, Injectable } from "@angular/core";
import { ZacQueryClient } from "../shared/http/zac-query-client";

@Injectable({
  providedIn: "root",
})
export class SignaleringenSettingsService {
  private readonly zacQueryClient = inject(ZacQueryClient);

  list() {
    return this.zacQueryClient.GET("/rest/signaleringen/instellingen");
  }

  put() {
    return this.zacQueryClient.PUT("/rest/signaleringen/instellingen");
  }
}
