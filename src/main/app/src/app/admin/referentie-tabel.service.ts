/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Injectable } from "@angular/core";
import { Observable } from "rxjs";
import {
  PostBody,
  PutBody,
  ZacHttpClient,
} from "../shared/http/zac-http-client";

@Injectable({
  providedIn: "root",
})
export class ReferentieTabelService {
  constructor(private readonly zacHttpClient: ZacHttpClient) {}

  listReferentieTabellen() {
    return this.zacHttpClient.GET("/rest/referentietabellen", {});
  }

  createReferentieTabel(body: PostBody<"/rest/referentietabellen">) {
    return this.zacHttpClient.POST("/rest/referentietabellen", body, {});
  }

  readReferentieTabel(id: number) {
    return this.zacHttpClient.GET("/rest/referentietabellen/{id}", {
      path: { id },
    });
  }

  readReferentieTabelByCode(code: string) {
    return this.zacHttpClient.GET("/rest/referentietabellen/code/{code}", {
      path: { code },
    });
  }

  updateReferentieTabel(
    id: number,
    body: PutBody<"/rest/referentietabellen/{id}">,
  ) {
    return this.zacHttpClient.PUT("/rest/referentietabellen/{id}", body, {
      path: { id },
    });
  }

  deleteReferentieTabel(id: number) {
    return this.zacHttpClient.DELETE("/rest/referentietabellen/{id}", {
      path: { id },
    });
  }

  listAfzenders() {
    return this.zacHttpClient.GET("/rest/referentietabellen/afzender", {});
  }

  listCommunicatiekanalen(inclusiefEFormulier?: boolean) {
    return this.zacHttpClient.GET(
      "/rest/referentietabellen/communicatiekanaal/{inclusiefEFormulier}",
      {
        path: { inclusiefEFormulier: inclusiefEFormulier ?? false },
      },
    );
  }

  listDomeinen() {
    return this.zacHttpClient.GET("/rest/referentietabellen/domein", {});
  }

  listServerErrorTexts(): Observable<string[]> {
    return this.zacHttpClient.GET(
      "/rest/referentietabellen/server-error-text",
      {},
    );
  }

  listBrpSearchValues() {
    return this.zacHttpClient.GET(
      "/rest/referentietabellen/brp-doelbinding-zoek-waarde",
      {},
    );
  }

  listBrpViewValues() {
    return this.zacHttpClient.GET(
      "/rest/referentietabellen/brp-doelbinding-raadpleeg-waarde",
      {},
    );
  }
}
