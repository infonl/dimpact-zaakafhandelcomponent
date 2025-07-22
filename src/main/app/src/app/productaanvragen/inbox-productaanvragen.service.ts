/*
 * SPDX-FileCopyrightText: 2023 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Injectable } from "@angular/core";
import { PutBody, ZacHttpClient } from "../shared/http/zac-http-client";

@Injectable({
  providedIn: "root",
})
export class InboxProductaanvragenService {
  private basepath = "/rest/inbox-productaanvragen";

  constructor(private readonly zacHttpClient: ZacHttpClient) {}

  list(body: PutBody<"/rest/inbox-productaanvragen">) {
    return this.zacHttpClient.PUT("/rest/inbox-productaanvragen", body);
  }

  delete(id: number) {
    return this.zacHttpClient.DELETE("/rest/inbox-productaanvragen/{id}", {
      path: { id },
    });
  }

  pdfPreview(aanvraagdocumentUUID: string): string {
    return `${this.basepath}/${aanvraagdocumentUUID}/pdfPreview`;
  }
}
