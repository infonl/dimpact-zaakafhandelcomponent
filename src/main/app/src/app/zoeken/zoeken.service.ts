/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { inject, Injectable, signal } from "@angular/core";
import { Subject } from "rxjs";
import { PathParameters, PutBody } from "../shared/http/http-client";
import { ZacHttpClient } from "../shared/http/zac-http-client";

type FindLinkableZakenParams = PathParameters<
  "/rest/zaken/gekoppelde-zaken/{zaakUuid}/zoek-koppelbare-zaken",
  "get"
>;

export const LINKABLE_ZAKEN_PAGINATION_SIZE = 10;

@Injectable({
  providedIn: "root",
})
export class ZoekenService {
  public readonly trefwoorden = signal<string | null>(null);
  public readonly hasSearched = signal(false);
  public reset$ = new Subject<void>();

  private readonly zacHttpClient = inject(ZacHttpClient);

  list(body: PutBody<"/rest/zoeken/list">) {
    return this.zacHttpClient.PUT("/rest/zoeken/list", body);
  }

  listDocumentKoppelbareZaken(body: PutBody<"/rest/zoeken/zaken">) {
    return this.zacHttpClient.PUT("/rest/zoeken/zaken", {
      ...body,
    });
  }

  findLinkableZaken({
    zaakUuid,
    relationType,
    zoekZaakIdentifier,
    zoekZaakOmschrijving,
    zoekZaakType,
  }: FindLinkableZakenParams["query"] & FindLinkableZakenParams["path"]) {
    return this.zacHttpClient.GET(
      "/rest/zaken/gekoppelde-zaken/{zaakUuid}/zoek-koppelbare-zaken",
      {
        path: { zaakUuid },
        query: {
          relationType,
          zoekZaakIdentifier: zoekZaakIdentifier || undefined,
          zoekZaakOmschrijving: zoekZaakOmschrijving || undefined,
          zoekZaakType: zoekZaakType || undefined,
          page: 0,
          rows: LINKABLE_ZAKEN_PAGINATION_SIZE,
        },
      },
    );
  }
}
