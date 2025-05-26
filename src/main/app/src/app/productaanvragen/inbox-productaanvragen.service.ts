/*
 * SPDX-FileCopyrightText: 2023 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Injectable } from "@angular/core";
import { ZacHttpClient } from "../shared/http/zac-http-client";
import { ListParameters } from "../shared/model/list-parameters";

@Injectable({
  providedIn: "root",
})
export class InboxProductaanvragenService {
  private basepath = "/rest/inbox-productaanvragen";

  constructor(private readonly zacHttpClient: ZacHttpClient) {}

  list(parameters: ListParameters) {
    return this.zacHttpClient.PUT("/rest/inbox-productaanvragen", parameters);
  }

  delete(id: number) {
    return this.zacHttpClient.DELETE("/rest/inbox-productaanvragen/{id}", {
      pathParams: { path: { id } },
    });
  }

  pdfPreview(aanvraagdocumentUUID: string): string {
    return `${this.basepath}/${aanvraagdocumentUUID}/pdfPreview`;
  }
}
