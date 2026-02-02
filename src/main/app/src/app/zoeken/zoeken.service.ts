/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { inject, Injectable, signal } from "@angular/core";
import { Subject } from "rxjs";
import { PutBody } from "../shared/http/http-client";
import { ZacHttpClient } from "../shared/http/zac-http-client";
import { GeneratedType } from "../shared/utils/generated-types";

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

  findLinkableZaken(
    zaakUuid: string,
    zoekZaakIdentifier: string,
    relationType: GeneratedType<"RelatieType">,
  ) {
    return this.zacHttpClient.GET(
      "/rest/zaken/gekoppelde-zaken/{zaakUuid}/zoek-koppelbare-zaken",
      {
        path: { zaakUuid },
        query: { zoekZaakIdentifier: zoekZaakIdentifier, relationType: relationType, page: 0, rows: 10},
      },
    );
  }
}
