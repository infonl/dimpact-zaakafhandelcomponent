/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { inject, Injectable } from "@angular/core";
import { QueryClient } from "@tanstack/angular-query-experimental";
import { lastValueFrom } from "rxjs";
import { tap } from "rxjs/operators";
import { PutBody } from "../shared/http/http-client";
import { ZacHttpClient } from "../shared/http/zac-http-client";
import { ZacQueryClient } from "../shared/http/zac-query-client";

@Injectable({
  providedIn: "root",
})
export class ReferentieTabelService {
  private readonly zacHttpClient = inject(ZacHttpClient);
  private readonly zacQueryClient = inject(ZacQueryClient);
  private readonly queryClient = inject(QueryClient, { optional: true });

  listReferentieTabellen() {
    return this.zacHttpClient.GET("/rest/referentietabellen");
  }

  listReferentieTabellenQuery() {
    return this.zacQueryClient.GET("/rest/referentietabellen");
  }

  readReferentieTabelQuery(id: number) {
    return this.zacQueryClient.GET("/rest/referentietabellen/{id}", {
      path: { id },
    });
  }

  private invalidateReferentieTabellen() {
    return this.queryClient?.invalidateQueries({
      queryKey: this.listReferentieTabellenQuery().queryKey,
    });
  }

  private invalidateReferentieTabel(id: number) {
    return Promise.all([
      this.invalidateReferentieTabellen(),
      this.queryClient?.invalidateQueries({
        queryKey: this.readReferentieTabelQuery(id).queryKey,
      }),
    ]);
  }

  createReferentieTabelMutation() {
    return {
      ...this.zacQueryClient.POST("/rest/referentietabellen"),
      onSuccess: () => void this.invalidateReferentieTabellen(),
    };
  }

  readReferentieTabelByCode(code: string) {
    return this.zacQueryClient.GET("/rest/referentietabellen/code/{code}", {
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

  // Cold observable (fires on subscribe), so it's safe to pass to ConfirmDialogData.
  updateReferentieTabelWithRefresh(
    id: number,
    body: PutBody<"/rest/referentietabellen/{id}">,
  ) {
    return this.updateReferentieTabel(id, body).pipe(
      tap(() => void this.invalidateReferentieTabel(id)),
    );
  }

  // Cold observable (fires on subscribe), so it's safe to pass to ConfirmDialogData.
  deleteReferentieTabelWithRefresh(id: number) {
    return this.deleteReferentieTabel(id).pipe(
      tap(() => void this.invalidateReferentieTabel(id)),
    );
  }

  updateReferentieTabelAsync(
    id: number,
    body: PutBody<"/rest/referentietabellen/{id}">,
  ) {
    return lastValueFrom(this.updateReferentieTabelWithRefresh(id, body));
  }

  listAfzenders() {
    return this.zacHttpClient.GET("/rest/referentietabellen/afzender");
  }

  listCommunicatiekanalen(inclusiefEFormulier?: boolean) {
    return this.zacHttpClient.GET(
      "/rest/referentietabellen/communicatiekanaal/{inclusiefEFormulier}",
      {
        path: { inclusiefEFormulier: inclusiefEFormulier ?? false },
      },
    );
  }

  listServerErrorTexts() {
    return this.zacHttpClient.GET("/rest/referentietabellen/server-error-text");
  }

  listBrpSearchValues() {
    return this.zacHttpClient.GET(
      "/rest/referentietabellen/brp-doelbinding-zoek-waarde",
    );
  }

  listBrpViewValues() {
    return this.zacHttpClient.GET(
      "/rest/referentietabellen/brp-doelbinding-raadpleeg-waarde",
    );
  }

  listBrpProcessingValues() {
    return this.zacHttpClient.GET(
      "/rest/referentietabellen/brp-verwerkingregister-waarde",
    );
  }
}
