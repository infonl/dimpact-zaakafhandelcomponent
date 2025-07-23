/*
 * SPDX-FileCopyrightText: 2023 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Injectable } from "@angular/core";
import {
  PostBody,
  PutBody,
  ZacHttpClient,
} from "../shared/http/zac-http-client";

@Injectable({
  providedIn: "root",
})
export class FormulierDefinitieService {
  constructor(private readonly zacHttpClient: ZacHttpClient) {}

  create(body: PostBody<"/rest/formulierdefinities">) {
    return this.zacHttpClient.POST("/rest/formulierdefinities", body);
  }

  read(id: number) {
    return this.zacHttpClient.GET("/rest/formulierdefinities/{id}", {
      path: { id },
    });
  }

  update(body: PutBody<"/rest/formulierdefinities">) {
    return this.zacHttpClient.PUT("/rest/formulierdefinities", body);
  }

  delete(id: number) {
    return this.zacHttpClient.DELETE("/rest/formulierdefinities/{id}", {
      path: { id },
    });
  }

  list() {
    return this.zacHttpClient.GET("/rest/formulierdefinities");
  }
}
