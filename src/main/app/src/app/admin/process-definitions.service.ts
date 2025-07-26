/*
 * SPDX-FileCopyrightText: 2024 Dimpact
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Injectable } from "@angular/core";
import { PostBody, ZacHttpClient } from "../shared/http/zac-http-client";

@Injectable({
  providedIn: "root",
})
export class ProcessDefinitionsService {
  constructor(private readonly zacHttpClient: ZacHttpClient) {}

  listProcessDefinitions() {
    return this.zacHttpClient.GET("/rest/process-definitions");
  }

  uploadProcessDefinition(body: PostBody<"/rest/process-definitions">) {
    return this.zacHttpClient.POST("/rest/process-definitions", body);
  }

  deleteProcessDefinition(key: string) {
    return this.zacHttpClient.DELETE("/rest/process-definitions/{key}", {
      path: { key },
    });
  }
}
