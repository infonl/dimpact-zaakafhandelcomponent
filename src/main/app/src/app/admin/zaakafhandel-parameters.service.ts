/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024-2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Injectable } from "@angular/core";
import { PutBody, ZacHttpClient } from "../shared/http/zac-http-client";

@Injectable({
  providedIn: "root",
})
export class ZaakafhandelParametersService {
  constructor(private readonly zacHttpClient: ZacHttpClient) {}

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

  updateZaakafhandelparameters(body: PutBody<"/rest/zaakafhandelparameters">) {
    return this.zacHttpClient.PUT("/rest/zaakafhandelparameters", body);
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
    return this.zacHttpClient.PUT(
      "/rest/zaakafhandelparameters/smartdocuments-group-template-names",
      body,
    );
  }
}
