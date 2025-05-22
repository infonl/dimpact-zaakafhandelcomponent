/*
 * SPDX-FileCopyrightText: 2023 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Injectable } from "@angular/core";
import { catchError } from "rxjs/operators";
import { FoutAfhandelingService } from "../fout-afhandeling/fout-afhandeling.service";
import { ZacHttpClient } from "../shared/http/zac-http-client";
import { ListParameters } from "../shared/model/list-parameters";

@Injectable({
  providedIn: "root",
})
export class InboxProductaanvragenService {
  private basepath = "/rest/inbox-productaanvragen";

  constructor(
    private readonly foutAfhandelingService: FoutAfhandelingService,
    private readonly zacHttpClient: ZacHttpClient,
  ) {}

  list(parameters: ListParameters) {
    return this.zacHttpClient
      .PUT("/rest/inbox-productaanvragen", parameters)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  delete(id: number) {
    return this.zacHttpClient
      .DELETE("/rest/inbox-productaanvragen/{id}", {
        pathParams: { path: { id } },
      })
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  pdfPreview(aanvraagdocumentUUID: string): string {
    return `${this.basepath}/${aanvraagdocumentUUID}/pdfPreview`;
  }
}
