/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024-2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Injectable } from "@angular/core";
import { ZacHttpClient } from "../shared/http/zac-http-client";

@Injectable({
  providedIn: "root",
})
export class BpmnConfigurationService {
  constructor(private readonly zacHttpClient: ZacHttpClient) {}

  listBpmnProcessDefintionConfigurations() {
    return this.zacHttpClient.GET("/rest/zaaktype-bpmn-configuration");
  }
}
