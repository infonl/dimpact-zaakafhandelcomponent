/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { inject, Injectable, signal } from "@angular/core";
import { QueryClient } from "@tanstack/angular-query-experimental";
import { Subject } from "rxjs";
import { PathParameters, PutBody } from "../shared/http/http-client";
import { runQuery } from "../shared/http/run-query";
import { ZacQueryClient } from "../shared/http/zac-query-client";

const ZOEK_KOPPELBARE_ZAKEN_PATH =
  "/rest/zaken/gekoppelde-zaken/{zaakUuid}/zoek-koppelbare-zaken" as const;

type FindLinkableZakenParams = PathParameters<
  typeof ZOEK_KOPPELBARE_ZAKEN_PATH,
  "put"
>["path"] &
  PutBody<typeof ZOEK_KOPPELBARE_ZAKEN_PATH>;

export const LINKABLE_ZAKEN_PAGINATION_SIZE = 10;

@Injectable({
  providedIn: "root",
})
export class ZoekenService {
  public readonly trefwoorden = signal<string | null>(null);
  public readonly hasSearched = signal(false);
  public reset$ = new Subject<void>();

  private readonly zacQueryClient = inject(ZacQueryClient);
  private readonly queryClient = inject(QueryClient);

  /**
   * For the table data sources, which are built outside an injection context and
   * so cannot reach the query client themselves.
   */
  list$(body: PutBody<"/rest/zoeken/list">) {
    return runQuery(this.queryClient, this.list(body));
  }

  list(body: PutBody<"/rest/zoeken/list">) {
    return this.zacQueryClient.PUT_QUERY("/rest/zoeken/list", body);
  }

  listDocumentKoppelbareZaken(body: PutBody<"/rest/zoeken/zaken">) {
    return this.zacQueryClient.PUT_QUERY("/rest/zoeken/zaken", {
      ...body,
    });
  }

  findLinkableZaken({
    zaakUuid,
    relationType,
    zoekZaakIdentifier,
    zoekZaakOmschrijving,
    zoekZaakTypeOmschrijving,
    startdatum,
    einddatum,
  }: Omit<FindLinkableZakenParams, "page" | "rows">) {
    return this.zacQueryClient.PUT_QUERY(
      ZOEK_KOPPELBARE_ZAKEN_PATH,
      {
        relationType,
        zoekZaakIdentifier: zoekZaakIdentifier || null,
        zoekZaakOmschrijving: zoekZaakOmschrijving || null,
        zoekZaakTypeOmschrijving: zoekZaakTypeOmschrijving || null,
        startdatum,
        einddatum,
        page: 0,
        rows: LINKABLE_ZAKEN_PAGINATION_SIZE,
      },
      {
        path: { zaakUuid },
      },
    );
  }
}
