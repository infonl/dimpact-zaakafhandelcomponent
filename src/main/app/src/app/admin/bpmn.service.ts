/*
 * SPDX-FileCopyrightText: 2024 Dimpact
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Injectable } from "@angular/core";
import { PostBody } from "../shared/http/http-client";
import { ZacHttpClient } from "../shared/http/zac-http-client";

@Injectable({
  providedIn: "root",
})
export class BpmnService {
  constructor(private readonly zacHttpClient: ZacHttpClient) {}

  listProcessDefinitions() {
    return this.zacHttpClient.GET("/rest/bpmn-process-definitions");
  }

  uploadProcessDefinition(body: PostBody<"/rest/bpmn-process-definitions">) {
    return this.zacHttpClient.POST("/rest/bpmn-process-definitions", body);
  }

  deleteProcessDefinition(key: string) {
    return this.zacHttpClient.DELETE("/rest/bpmn-process-definitions/{key}", {
      path: { key },
    });
  }
}
