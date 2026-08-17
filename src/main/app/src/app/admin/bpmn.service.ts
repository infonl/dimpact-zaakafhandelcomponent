/*
 * SPDX-FileCopyrightText: 2024 Dimpact, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { inject, Injectable } from "@angular/core";
import { UtilService } from "../core/service/util.service";
import { PostBody } from "../shared/http/http-client";
import { mergeMutationOptions } from "../shared/http/merge-mutation-options";
import { ZacHttpClient } from "../shared/http/zac-http-client";
import { ZacQueryClient } from "../shared/http/zac-query-client";

@Injectable({
  providedIn: "root",
})
export class BpmnService {
  private readonly zacHttpClient = inject(ZacHttpClient);
  private readonly zacQueryClient = inject(ZacQueryClient);
  private readonly utilService = inject(UtilService);

  listProcessDefinitionsQuery(details: boolean = false) {
    return this.zacQueryClient.GET("/rest/bpmn-process-definitions", {
      query: { details },
    });
  }

  uploadProcessDefinitionQuery() {
    return this.zacQueryClient.POST("/rest/bpmn-process-definitions");
  }

  deleteProcessDefinition(processDefinition: { key: string; name: string }) {
    return mergeMutationOptions(
      this.zacQueryClient.DELETE("/rest/bpmn-process-definitions/{key}", {
        path: { key: processDefinition.key },
      }),
      {
        onSuccess: () =>
          this.utilService.openSnackbar("msg.bpmn.process-definition.deleted", {
            naam: processDefinition.name,
          }),
      },
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
    return this.zacQueryClient.DELETE(
      "/rest/bpmn-process-definitions/{key}/forms/{name}",
      {
        path: { key: processDefinitionKey, name },
      },
    );
  }
}
