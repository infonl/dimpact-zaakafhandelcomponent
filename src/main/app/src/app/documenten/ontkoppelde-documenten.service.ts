/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { inject, Injectable } from "@angular/core";
import { PutBody } from "../shared/http/http-client";
import { ZacHttpClient } from "../shared/http/zac-http-client";
import { ZacQueryClient } from "../shared/http/zac-query-client";

@Injectable({
  providedIn: "root",
})
export class OntkoppeldeDocumentenService {
  private readonly zacHttpClient = inject(ZacHttpClient);
  private readonly zacQueryClient = inject(ZacQueryClient);

  list(body: PutBody<"/rest/ontkoppeldedocumenten">) {
    return this.zacHttpClient.PUT("/rest/ontkoppeldedocumenten", body);
  }

  delete(id: number) {
    return this.zacQueryClient.DELETE("/rest/ontkoppeldedocumenten/{id}", {
      path: { id },
    });
  }
}
