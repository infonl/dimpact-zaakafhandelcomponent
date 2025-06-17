/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Injectable } from "@angular/core";
import { PutBody, ZacHttpClient } from "../shared/http/zac-http-client";

@Injectable({
  providedIn: "root",
})
export class InboxDocumentenService {
  constructor(private readonly zacHttpClient: ZacHttpClient) {}

  list(body: PutBody<"/rest/inboxdocumenten">) {
    return this.zacHttpClient.PUT("/rest/inboxdocumenten", body, {});
  }

  delete(id: number) {
    return this.zacHttpClient.DELETE("/rest/inboxdocumenten/{id}", {
      path: { id },
    });
  }
}
