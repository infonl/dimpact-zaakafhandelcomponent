/*
 * SPDX-FileCopyrightText: 2024 Dimpact
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Injectable } from "@angular/core";
import { Observable } from "rxjs";
import { PostBody, ZacHttpClient } from "../shared/http/zac-http-client";

@Injectable({
  providedIn: "root",
})
export class FormioFormulierenService {
  constructor(private readonly zacHttpClient: ZacHttpClient) {}

  listFormioFormulieren() {
    return this.zacHttpClient.GET("/rest/formio-formulieren");
  }

  uploadFormioFormulier(
    body: PostBody<"/rest/formio-formulieren">,
  ): Observable<void> {
    return this.zacHttpClient.POST("/rest/formio-formulieren", body);
  }

  deleteFormioFormulier(id: number) {
    return this.zacHttpClient.DELETE("/rest/formio-formulieren/{id}", {
      path: { id },
    });
  }
}
