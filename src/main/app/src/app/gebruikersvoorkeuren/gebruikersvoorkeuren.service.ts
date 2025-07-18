/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Injectable } from "@angular/core";
import { Observable } from "rxjs";
import { catchError } from "rxjs/operators";

import { HttpClient } from "@angular/common/http";
import { FoutAfhandelingService } from "../fout-afhandeling/fout-afhandeling.service";
import { TabelGegevens } from "../shared/dynamic-table/model/tabel-gegevens";
import {
  DeleteBody,
  PutBody,
  ZacHttpClient,
} from "../shared/http/zac-http-client";
import { GeneratedType } from "../shared/utils/generated-types";
import { Werklijst } from "./model/werklijst";
import { Zoekopdracht } from "./model/zoekopdracht";

@Injectable({
  providedIn: "root",
})
export class GebruikersvoorkeurenService {
  private basepath = "/rest/gebruikersvoorkeuren";

  constructor(
    private http: HttpClient,
    private foutAfhandelingService: FoutAfhandelingService,
    private readonly zacHttpClient: ZacHttpClient,
  ) {}

  listZoekOpdrachten(werklijst: Werklijst): Observable<Zoekopdracht[]> {
    return this.http
      .get<Zoekopdracht[]>(`${this.basepath}/zoekopdracht/${werklijst}`)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  createOrUpdateZoekOpdrachten(
    zoekopdracht: Zoekopdracht,
  ): Observable<Zoekopdracht> {
    return this.http
      .post<Zoekopdracht>(`${this.basepath}/zoekopdracht`, zoekopdracht)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  deleteZoekOpdrachten(id: number): Observable<void> {
    return this.http
      .delete<void>(`${this.basepath}/zoekopdracht/${id}`)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  setZoekopdrachtActief(zoekopdracht: Zoekopdracht): Observable<void> {
    return this.http
      .put<void>(`${this.basepath}/zoekopdracht/actief`, zoekopdracht)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  removeZoekopdrachtActief(werklijst: Werklijst): Observable<void> {
    return this.http
      .delete<void>(`${this.basepath}/zoekopdracht/${werklijst}/actief`)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  readTabelGegevens(werklijst: Werklijst): Observable<TabelGegevens> {
    return this.http
      .get<TabelGegevens>(`${this.basepath}/tabel-gegevens/${werklijst}`)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  updateAantalPerPagina(
    werklijst: GeneratedType<"Werklijst">,
    aantal: number,
  ): Observable<void> {
    return this.http
      .put<void>(
        `${this.basepath}/aantal-per-pagina/${werklijst}/${aantal}`,
        {},
      )
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  listDashboardCards() {
    return this.zacHttpClient.GET(
      "/rest/gebruikersvoorkeuren/dasboardcard/actief",
      {},
    );
  }

  updateDashboardCards(
    body: PutBody<"/rest/gebruikersvoorkeuren/dasboardcard/actief">,
  ) {
    return this.zacHttpClient.PUT(
      "/rest/gebruikersvoorkeuren/dasboardcard/actief",
      body,
      {},
    );
  }

  addDashboardCard(body: PutBody<"/rest/gebruikersvoorkeuren/dasboardcard">) {
    return this.zacHttpClient.PUT(
      "/rest/gebruikersvoorkeuren/dasboardcard",
      body,
      {},
    );
  }

  deleteDashboardCard(
    body: DeleteBody<"/rest/gebruikersvoorkeuren/dasboardcard">,
  ) {
    return this.zacHttpClient.DELETE(
      "/rest/gebruikersvoorkeuren/dasboardcard",
      {},
      body,
    );
  }
}
