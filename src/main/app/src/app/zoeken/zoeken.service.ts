/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { Observable, Subject } from "rxjs";
import { catchError } from "rxjs/operators";
import { FoutAfhandelingService } from "../fout-afhandeling/fout-afhandeling.service";
import { ZoekObject } from "./model/zoek-object";
import { ZoekParameters } from "./model/zoek-parameters";
import { ZoekResultaat } from "./model/zoek-resultaat";
import { GeneratedType } from "../shared/utils/generated-types";
import { Resultaat } from "../shared/model/resultaat";

export type KoppelbareZaakListItem = {
  foutmelding: string;
  resultaten: Array<{
    documentKoppelen: boolean;
    id: string;
    identificatie: string;
    omschrijving: string;
    toelichting: string;
    type: string;
  }>;
  totaal: number;
  filters: Record<string, unknown>;
};

@Injectable({
  providedIn: "root",
})
export class ZoekenService {
  private basepath = "/rest/zoeken";
  public trefwoorden$ = new Subject<string>();
  public hasSearched$ = new Subject<boolean>();
  public reset$ = new Subject<void>();

  constructor(
    private http: HttpClient,
    private foutAfhandelingService: FoutAfhandelingService,
  ) {}

  list(zoekParameters: ZoekParameters): Observable<ZoekResultaat<ZoekObject>> {
    return this.http
      .put<ZoekResultaat<ZoekObject>>(`${this.basepath}/list`, zoekParameters)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  listKoppelbareZaken(
    zaakIdentificator: string,
    documentUUID: string,
  ): Observable<Resultaat<KoppelbareZaakListItem>> {
    return this.http
      .put<Resultaat<KoppelbareZaakListItem>>(`${this.basepath}/zaken`, {
        zaakIdentificator,
        documentUUID,
        page: 0,
        rows: 10,
      })
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }
}
