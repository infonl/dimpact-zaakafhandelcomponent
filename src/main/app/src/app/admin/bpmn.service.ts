/*
 * SPDX-FileCopyrightText: 2024 Dimpact, 2026 INFO.nl
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
    return this.zacHttpClient.GET("/rest/bpmn-process-definitions", {
      query: { details: true },
    });
  }

  uploadProcessDefinition(body: PostBody<"/rest/bpmn-process-definitions">) {
    return this.zacHttpClient.POST("/rest/bpmn-process-definitions", body);
  }

  deleteProcessDefinition(key: string) {
    return this.zacHttpClient.DELETE("/rest/bpmn-process-definitions/{key}", {
      path: { key },
    });
  }

  listFormioFormulieren() {
    return this.zacHttpClient.GET("/rest/formio-formulieren");
  }

  uploadProcessDefinitionForm(
    key: string,
    body: PostBody<"/rest/bpmn-process-definitions/{key}/forms">,
  ) {
    return this.zacHttpClient.POST(
      "/rest/bpmn-process-definitions/{key}/forms",
      body,
      { path: { key } },
    );
  }

  deleteProcessDefinitionForm(key: string, name: string) {
    return this.zacHttpClient.DELETE(
      "/rest/bpmn-process-definitions/{key}/forms/{name}",
      {
        path: { key, name },
      },
    );
  }
}
