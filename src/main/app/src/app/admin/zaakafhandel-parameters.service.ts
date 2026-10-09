/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024-2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { inject, Injectable } from "@angular/core";
import { QueryClient } from "@tanstack/angular-query-experimental";
import { UtilService } from "../core/service/util.service";
import { PutBody } from "../shared/http/http-client";
import { mergeMutationOptions } from "../shared/http/merge-mutation-options";
import { ZacHttpClient } from "../shared/http/zac-http-client";
import { ZacQueryClient } from "../shared/http/zac-query-client";
import { MailtemplateKoppelingService } from "./mailtemplate-koppeling.service";

@Injectable({
  providedIn: "root",
})
export class ZaakafhandelParametersService {
  private readonly zacHttpClient = inject(ZacHttpClient);
  private readonly zacQueryClient = inject(ZacQueryClient);
  private readonly utilService = inject(UtilService);
  private readonly mailtemplateKoppelingService = inject(
    MailtemplateKoppelingService,
  );
  private readonly queryClient = inject(QueryClient, { optional: true });

  listZaakafhandelParameters() {
    return this.zacHttpClient.GET("/rest/zaakafhandelparameters");
  }

  readZaakafhandelparameters(zaaktypeUUID: string) {
    return this.zacHttpClient.GET(
      "/rest/zaakafhandelparameters/{zaaktypeUUID}",
      {
        path: { zaaktypeUUID },
      },
    );
  }

  listZaakbeeindigRedenen() {
    return this.zacHttpClient.GET(
      "/rest/zaakafhandelparameters/zaakbeeindigredenen",
    );
  }

  listZaakbeeindigRedenenForZaaktype(zaaktypeUUID: string) {
    return this.zacHttpClient.GET(
      "/rest/zaakafhandelparameters/zaakbeeindigredenen/{zaaktypeUUID}",
      {
        path: { zaaktypeUUID },
      },
    );
  }

  listResultaattypes(zaaktypeUUID: string) {
    return this.zacHttpClient.GET(
      "/rest/zaakafhandelparameters/resultaattypes/{zaaktypeUUID}",
      {
        path: { zaaktypeUUID },
      },
    );
  }

  listCaseDefinitions() {
    return this.zacHttpClient.GET(
      "/rest/zaakafhandelparameters/case-definitions",
    );
  }

  updateZaakafhandelparameters() {
    return mergeMutationOptions(
      this.zacQueryClient.PUT("/rest/zaakafhandelparameters"),
      {
        onSuccess: () => {
          void this.queryClient?.invalidateQueries({
            queryKey:
              this.mailtemplateKoppelingService.listMailtemplateKoppelingen()
                .queryKey,
          });
          this.utilService.openSnackbar(
            "msg.zaakafhandelparameters.opgeslagen",
          );
        },
      },
    );
  }

  getZaaktypeBpmnConfiguration() {
    return this.zacHttpClient.GET("/rest/zaaktype-bpmn-configuration");
  }

  createOrUpdateBpmnZaakafhandelparameters() {
    return mergeMutationOptions(
      this.zacQueryClient.POST("/rest/zaaktype-bpmn-configuration"),
      {
        onSuccess: () =>
          this.utilService.openSnackbar(
            "msg.zaakafhandelparameters.opgeslagen",
          ),
      },
    );
  }

  listFormulierDefinities() {
    return this.zacHttpClient.GET(
      "/rest/zaakafhandelparameters/formulierdefinities",
    );
  }

  listReplyTos() {
    return this.zacHttpClient.GET("/rest/zaakafhandelparameters/replyTo");
  }

  listSmartDocumentsGroupTemplateNames(
    body: PutBody<"/rest/zaakafhandelparameters/smartdocuments-group-template-names">,
  ) {
    return this.zacQueryClient.PUT_QUERY(
      "/rest/zaakafhandelparameters/smartdocuments-group-template-names",
      body,
    );
  }
}
