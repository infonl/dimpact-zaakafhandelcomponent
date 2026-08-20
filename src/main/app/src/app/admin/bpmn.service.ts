/*
 * SPDX-FileCopyrightText: 2024 Dimpact, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { inject, Injectable } from "@angular/core";
import { QueryClient } from "@tanstack/angular-query-experimental";
import { UtilService } from "../core/service/util.service";
import { PostBody } from "../shared/http/http-client";
import { mergeMutationOptions } from "../shared/http/merge-mutation-options";
import { ZacHttpClient } from "../shared/http/zac-http-client";
import { ZacQueryClient } from "../shared/http/zac-query-client";

const PROCESS_DEFINITIONS_PATH = "/rest/bpmn-process-definitions";

@Injectable({
  providedIn: "root",
})
export class BpmnService {
  private readonly zacHttpClient = inject(ZacHttpClient);
  private readonly zacQueryClient = inject(ZacQueryClient);
  private readonly utilService = inject(UtilService);
  private readonly queryClient = inject(QueryClient, { optional: true });

  listProcessDefinitionsQuery(details: boolean = false) {
    return this.zacQueryClient.GET(PROCESS_DEFINITIONS_PATH, {
      query: { details },
    });
  }

  /** Matches every `details` variant of {@link listProcessDefinitionsQuery}. */
  private invalidateProcessDefinitions() {
    return this.queryClient?.invalidateQueries({
      queryKey: [PROCESS_DEFINITIONS_PATH],
    });
  }

  uploadProcessDefinitionQuery() {
    return mergeMutationOptions(
      this.zacQueryClient.POST(PROCESS_DEFINITIONS_PATH),
      { onSuccess: () => void this.invalidateProcessDefinitions() },
    );
  }

  deleteProcessDefinition() {
    return mergeMutationOptions(
      this.zacQueryClient.DELETE(
        "/rest/bpmn-process-definitions/{key}",
        (processDefinition: { key: string; name: string }) => ({
          parameters: { path: { key: processDefinition.key } },
        }),
      ),
      {
        onSuccess: (_data, processDefinition) => {
          void this.invalidateProcessDefinitions();
          this.utilService.openSnackbar("msg.bpmn.process-definition.deleted", {
            naam: processDefinition.name,
          });
        },
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

  deleteProcessDefinitionForm() {
    return mergeMutationOptions(
      this.zacQueryClient.DELETE(
        "/rest/bpmn-process-definitions/{key}/forms/{name}",
        (processDefinitionForm: {
          processDefinitionKey: string;
          name: string;
        }) => ({
          parameters: {
            path: {
              key: processDefinitionForm.processDefinitionKey,
              name: processDefinitionForm.name,
            },
          },
        }),
      ),
      { onSuccess: () => void this.invalidateProcessDefinitions() },
    );
  }
}
