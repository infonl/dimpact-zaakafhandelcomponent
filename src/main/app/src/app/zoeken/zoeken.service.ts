/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { Observable, Subject } from "rxjs";
import { catchError } from "rxjs/operators";
import { FoutAfhandelingService } from "../fout-afhandeling/fout-afhandeling.service";
import { ZacHttpClient } from "../shared/http/zac-http-client";
import { Resultaat } from "../shared/model/resultaat";
import { GeneratedType } from "../shared/utils/generated-types";
import { ZoekObject } from "./model/zoek-object";
import { ZoekParameters } from "./model/zoek-parameters";
import { ZoekResultaat } from "./model/zoek-resultaat";

@Injectable({
  providedIn: "root",
})
export class ZoekenService {
  private basepath = "/rest/zoeken";
  public trefwoorden$ = new Subject<string>();
  public hasSearched$ = new Subject<boolean>();
  public reset$ = new Subject<void>();

  constructor(
    private readonly http: HttpClient,
    private readonly zacHttpClient: ZacHttpClient,
    private readonly foutAfhandelingService: FoutAfhandelingService,
  ) {}

  list(zoekParameters: ZoekParameters): Observable<ZoekResultaat<ZoekObject>> {
    return this.http
      .put<ZoekResultaat<ZoekObject>>(`${this.basepath}/list`, zoekParameters)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  listDocumentKoppelbareZaken(
    zaakIdentificator: string,
    informationObjectTypeUuid: string,
  ): Observable<Resultaat<GeneratedType<"RestZaakKoppelenZoekObject">>> {
    return this.http
      .put<Resultaat<GeneratedType<"RestZaakKoppelenZoekObject">>>(
        `${this.basepath}/zaken`,
        {
          zaakIdentificator,
          informationObjectTypeUuid,
          page: 0,
          rows: 10,
        },
      )
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
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
