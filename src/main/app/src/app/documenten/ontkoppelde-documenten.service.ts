/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Injectable } from "@angular/core";
import { PutBody, ZacHttpClient } from "../shared/http/zac-http-client";

@Injectable({
  providedIn: "root",
})
export class OntkoppeldeDocumentenService {
  constructor(private readonly zacHttpClient: ZacHttpClient) {}

  list(body: PutBody<"/rest/ontkoppeldedocumenten">) {
    return this.zacHttpClient.PUT("/rest/ontkoppeldedocumenten", body, {});
  }

  delete(id: number) {
    return this.zacHttpClient.DELETE("/rest/ontkoppeldedocumenten/{id}", {
      path: { id },
    });
  }
}
