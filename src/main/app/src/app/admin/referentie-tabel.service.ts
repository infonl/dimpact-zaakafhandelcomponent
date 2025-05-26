/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { Observable } from "rxjs";
import { catchError } from "rxjs/operators";
import { FoutAfhandelingService } from "../fout-afhandeling/fout-afhandeling.service";
import { ZacHttpClient } from "../shared/http/zac-http-client";
import { ReferentieTabel } from "./model/referentie-tabel";

@Injectable({
  providedIn: "root",
})
export class ReferentieTabelService {
  private basepath = "/rest/referentietabellen";

  constructor(
    private http: HttpClient,
    private foutAfhandelingService: FoutAfhandelingService,
    private readonly zacHttpClient: ZacHttpClient,
  ) {}

  listReferentieTabellen(): Observable<ReferentieTabel[]> {
    return this.http
      .get<ReferentieTabel[]>(`${this.basepath}`)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  createReferentieTabel(tabel: ReferentieTabel): Observable<ReferentieTabel> {
    return this.http
      .post<ReferentieTabel>(`${this.basepath}`, tabel)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  readReferentieTabel(id: string): Observable<ReferentieTabel> {
    return this.http
      .get<ReferentieTabel>(`${this.basepath}/${id}`)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  updateReferentieTabel(tabel: ReferentieTabel): Observable<ReferentieTabel> {
    return this.http
      .put<ReferentieTabel>(`${this.basepath}/${tabel.id}`, tabel)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  deleteReferentieTabel(id: number): Observable<void> {
    return this.http
      .delete<void>(`${this.basepath}/${id}`)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  listAfzenders(): Observable<string[]> {
    return this.http
      .get<string[]>(`${this.basepath}/afzender`)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  listCommunicatiekanalen(inclusiefEFormulier?: boolean): Observable<string[]> {
    return this.http
      .get<
        string[]
      >(`${this.basepath}/communicatiekanaal/${inclusiefEFormulier}`)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  listDomeinen(): Observable<string[]> {
    return this.http
      .get<string[]>(`${this.basepath}/domein`)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  listServerErrorTexts(): Observable<string[]> {
    return this.http
      .get<string[]>(`${this.basepath}/server-error-text`)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  listBrpSearchValues() {
    return this.zacHttpClient.GET(
      "/rest/referentietabellen/brp-doelbinding-zoek-waarde",
      {},
    );
  }

  listBrpViewValues() {
    return this.zacHttpClient.GET(
      "/rest/referentietabellen/brp-doelbinding-raadpleeg-waarde",
      {},
    );
  }
}
