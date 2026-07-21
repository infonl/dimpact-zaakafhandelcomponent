/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { inject, Injectable, signal } from "@angular/core";
import { Subject } from "rxjs";
import { PathParameters, PutBody } from "../shared/http/http-client";
import { ZacHttpClient } from "../shared/http/zac-http-client";

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
    startdatum,
    einddatum,
  }: Omit<FindLinkableZakenParams, "page" | "rows">) {
    return this.zacHttpClient.PUT(
      ZOEK_KOPPELBARE_ZAKEN_PATH,
      {
        relationType,
        zoekZaakIdentifier: zoekZaakIdentifier || null,
        zoekZaakOmschrijving: zoekZaakOmschrijving || null,
        zoekZaakType: zoekZaakType || null,
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
