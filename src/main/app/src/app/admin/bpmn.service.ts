/*
 * SPDX-FileCopyrightText: 2024 Dimpact, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { inject, Injectable } from "@angular/core";
import { lastValueFrom } from "rxjs";
import { PostBody } from "../shared/http/http-client";
import { ZacHttpClient } from "../shared/http/zac-http-client";
import { ZacQueryClient } from "../shared/http/zac-query-client";

@Injectable({
  providedIn: "root",
})
export class BpmnService {
  private readonly zacHttpClient = inject(ZacHttpClient);
  private readonly zacQueryClient = inject(ZacQueryClient);

  listProcessDefinitions(details: boolean = false) {
    return this.zacQueryClient.GET("/rest/bpmn-process-definitions", {
      query: { details },
    });
  }

  uploadProcessDefinition() {
    return this.zacQueryClient.POST("/rest/bpmn-process-definitions");
  }

  deleteProcessDefinition(key: string) {
    return lastValueFrom(
      this.zacHttpClient.DELETE("/rest/bpmn-process-definitions/{key}", {
        path: { key },
      }),
    );
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

  deleteProcessDefinitionForm(processDefinitionKey: string, name: string) {
    const key = processDefinitionKey;

    return this.zacHttpClient.DELETE(
      "/rest/bpmn-process-definitions/{key}/forms/{name}",
      {
        path: { key, name },
      },
    );
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
