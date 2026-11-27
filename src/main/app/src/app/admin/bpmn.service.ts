/*
 * SPDX-FileCopyrightText: 2024 Dimpact, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HttpErrorResponse, HttpResponse } from "@angular/common/http";
import { inject, Injectable } from "@angular/core";
import { queryOptions } from "@tanstack/angular-query-experimental";
import { UtilService } from "../core/service/util.service";
import { PathParameters } from "../shared/http/http-client";
import { mergeMutationOptions } from "../shared/http/merge-mutation-options";
import {
  LOAD_PER_VISIT,
  ZacQueryClient,
} from "../shared/http/zac-query-client";

const PROCESS_DEFINITIONS_PATH = "/rest/bpmn-process-definitions";

@Injectable({
  providedIn: "root",
})
export class BpmnService {
  private readonly zacQueryClient = inject(ZacQueryClient);
  private readonly utilService = inject(UtilService);

  listProcessDefinitionsQuery(details: boolean = false) {
    return this.zacQueryClient.GET(PROCESS_DEFINITIONS_PATH, {
      query: { details },
    });
  }

  uploadProcessDefinitionQuery() {
    return this.zacQueryClient.POST(PROCESS_DEFINITIONS_PATH);
  }

  /**
   * Each download hits the server, and a failure is reported by whoever
   * fetches this, instead of by the generic error dialog as well.
   */
  downloadProcessDefinition(key: string) {
    const { queryKey, queryFn } = this.zacQueryClient.GET(
      "/rest/bpmn-process-definitions/{key}/download",
      {
        path: { key },
        responseType: "blob",
        observe: "response",
      } as PathParameters<
        "/rest/bpmn-process-definitions/{key}/download",
        "get"
      > &
        Record<string, unknown>,
    );

    return queryOptions<HttpResponse<Blob>, HttpErrorResponse>({
      queryKey,
      queryFn: queryFn as unknown as () => Promise<HttpResponse<Blob>>,
      ...LOAD_PER_VISIT,
      retry: false,
      meta: { reportErrors: false },
    });
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
          this.utilService.openSnackbar("msg.bpmn.process-definition.deleted", {
            naam: processDefinition.name,
          });
        },
      },
    );
  }

  uploadProcessDefinitionForm(key: string) {
    return this.zacQueryClient.POST(
      "/rest/bpmn-process-definitions/{key}/forms",
      { path: { key } },
    );
  }

  deleteProcessDefinitionForm() {
    return this.zacQueryClient.DELETE(
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
    );
  }
}
