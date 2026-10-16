/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024-2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { inject, Injectable } from "@angular/core";
import { QueryClient } from "@tanstack/angular-query-experimental";
import { UtilService } from "../core/service/util.service";
import { MailtemplateService } from "../mailtemplate/mailtemplate.service";
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
  private readonly mailtemplateService = inject(MailtemplateService);
  private readonly queryClient = inject(QueryClient, { optional: true });

  listZaakafhandelParameters() {
    return this.zacHttpClient.GET("/rest/zaakafhandelparameters");
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
    return this.zacHttpClient.GET(
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
          void this.queryClient?.invalidateQueries({
            queryKey: this.readZaakafhandelparameters(zaaktypeUUID).queryKey,
          });
          void this.queryClient?.invalidateQueries({
            queryKey:
              this.mailtemplateKoppelingService.listMailtemplateKoppelingen()
                .queryKey,
          });
          void this.mailtemplateService.invalidateFoundMailtemplates();
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
