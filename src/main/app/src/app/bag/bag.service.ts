/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { catchError, Observable } from "rxjs";
import { FoutAfhandelingService } from "../fout-afhandeling/fout-afhandeling.service";
import { Resultaat } from "../shared/model/resultaat";
import { Adres } from "./model/adres";
import { BAGObject } from "./model/bagobject";
import { BAGObjectGegevens } from "./model/bagobject-gegevens";
import { ListAdressenParameters } from "./model/list-adressen-parameters";

@Injectable({
  providedIn: "root",
})
export class BAGService {
  constructor(
    private http: HttpClient,
    private foutAfhandelingService: FoutAfhandelingService,
  ) {}

  private basepath = "/rest/bag";

  listAdressen(
    listAdressenParameters: ListAdressenParameters,
  ): Observable<Resultaat<Adres>> {
    return this.http
      .put<Resultaat<Adres>>(`${this.basepath}/adres`, listAdressenParameters)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  create(bagObjectGegevens: BAGObjectGegevens): Observable<void> {
    return this.http
      .post<void>(`${this.basepath}`, bagObjectGegevens)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  list(zaakUuid: string): Observable<BAGObjectGegevens[]> {
    return this.http
      .get<BAGObjectGegevens[]>(`${this.basepath}/zaak/${zaakUuid}`)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  delete(bagObjectGegevens: BAGObjectGegevens): Observable<void> {
    return this.http
      .delete<void>(this.basepath, { body: bagObjectGegevens })
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  read(type: string, id: string): Observable<BAGObject> {
    return this.http
      .get<BAGObject>(`${this.basepath}/${type}/${id}`)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }
}
