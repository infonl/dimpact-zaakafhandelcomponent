/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Injectable } from "@angular/core";
import { Subject } from "rxjs";
import { PutBody, ZacHttpClient } from "../shared/http/zac-http-client";
import { GeneratedType } from "../shared/utils/generated-types";

@Injectable({
  providedIn: "root",
})
export class ZoekenService {
  public trefwoorden$ = new Subject<string>();
  public hasSearched$ = new Subject<boolean>();
  public reset$ = new Subject<void>();

  constructor(private readonly zacHttpClient: ZacHttpClient) {}

  list(body: PutBody<"/rest/zoeken/list">) {
    return this.zacHttpClient.PUT("/rest/zoeken/list", body, {});
  }

  listDocumentKoppelbareZaken(body: PutBody<"/rest/zoeken/zaken">) {
    return this.zacHttpClient.PUT(
      "/rest/zoeken/zaken",
      { page: 0, rows: 10, ...body },
      {},
    );
  }

  findLinkableZaken(
    zaakUuid: string,
    zoekZaakIdentifier: string,
    relationType: GeneratedType<"RelatieType">,
  ) {
    return this.zacHttpClient.GET(
      "/rest/zaken/gekoppelde-zaken/{zaakUuid}/zoek-koppelbare-zaken",
      {
        path: { zaakUuid },
        query: { zoekZaakIdentifier, relationType },
      },
    );
  }
}
