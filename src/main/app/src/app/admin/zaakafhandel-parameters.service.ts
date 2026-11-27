/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024-2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { inject, Injectable } from "@angular/core";
import { QueryClient } from "@tanstack/angular-query-experimental";
import { UtilService } from "../core/service/util.service";
import { PathParameters, PutBody } from "../shared/http/http-client";
import { mergeMutationOptions } from "../shared/http/merge-mutation-options";
import { ZacQueryClient } from "../shared/http/zac-query-client";

export type ZaakafhandelParametersListQuery = NonNullable<
  NonNullable<PathParameters<"/rest/zaakafhandelparameters", "get">>["query"]
>;

@Injectable({
  providedIn: "root",
})
export class ZaakafhandelParametersService {
  private readonly zacQueryClient = inject(ZacQueryClient);
  private readonly utilService = inject(UtilService);
  private readonly queryClient = inject(QueryClient, { optional: true });

  private readonly zaakafhandelParametersListUrl =
    "/rest/zaakafhandelparameters" as const;

  listZaakafhandelParameters(query: ZaakafhandelParametersListQuery) {
    return this.zacQueryClient.GET(this.zaakafhandelParametersListUrl, {
      query,
    });
  }

  invalidateZaakafhandelParametersList() {
    return this.queryClient?.invalidateQueries({
      queryKey: this.zacQueryClient.GET(this.zaakafhandelParametersListUrl)
        .queryKey,
    });
  }

  readZaakafhandelparameters(zaaktypeUUID: string) {
    return this.zacQueryClient.GET(
      "/rest/zaakafhandelparameters/{zaaktypeUUID}",
      {
        path: { zaaktypeUUID },
      },
    );
  }

  listZaakbeeindigRedenen() {
    return this.zacQueryClient.GET(
      "/rest/zaakafhandelparameters/zaakbeeindigredenen",
    );
  }

  listZaakbeeindigRedenenForZaaktype(zaaktypeUUID: string) {
    return this.zacQueryClient.GET(
      "/rest/zaakafhandelparameters/zaakbeeindigredenen/{zaaktypeUUID}",
      {
        path: { zaaktypeUUID },
      },
    );
  }

  listResultaattypes(zaaktypeUUID: string) {
    return this.zacQueryClient.GET(
      "/rest/zaakafhandelparameters/resultaattypes/{zaaktypeUUID}",
      {
        path: { zaaktypeUUID },
      },
    );
  }

  listProcessDefinitions() {
    return this.zacQueryClient.GET(
      "/rest/zaakafhandelparameters/process-definitions",
    );
  }

  updateZaakafhandelparameters(zaaktypeUUID: string) {
    return mergeMutationOptions(
      this.zacQueryClient.PUT("/rest/zaakafhandelparameters/{zaaktypeUUID}", {
        path: { zaaktypeUUID },
      }),
      {
        onSuccess: () => {
          this.utilService.openSnackbar(
            "msg.zaakafhandelparameters.opgeslagen",
          );
        },
      },
    );
  }

  listFormulierDefinities() {
    return this.zacQueryClient.GET(
      "/rest/zaakafhandelparameters/formulierdefinities",
    );
  }

  listReplyTos() {
    return this.zacQueryClient.GET("/rest/zaakafhandelparameters/replyTo");
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
