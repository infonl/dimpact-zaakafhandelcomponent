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
  PostBody,
  PutBody,
  ZacHttpClient,
} from "../shared/http/zac-http-client";
import { GeneratedType } from "../shared/utils/generated-types";

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

  listZoekOpdrachten(lijstID: GeneratedType<"Werklijst">) {
    return this.zacHttpClient.GET(
      "/rest/gebruikersvoorkeuren/zoekopdracht/{lijstID}",
      {
        path: { lijstID },
      },
    );
  }

  createOrUpdateZoekOpdrachten(
    body: PostBody<"/rest/gebruikersvoorkeuren/zoekopdracht">,
  ) {
    return this.zacHttpClient.POST(
      "/rest/gebruikersvoorkeuren/zoekopdracht",
      body,
    );
  }

  deleteZoekOpdrachten(id: number): Observable<void> {
    return this.http
      .delete<void>(`${this.basepath}/zoekopdracht/${id}`)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  setZoekopdrachtActief(
    body: PutBody<"/rest/gebruikersvoorkeuren/zoekopdracht/actief">,
  ) {
    return this.zacHttpClient.PUT(
      "/rest/gebruikersvoorkeuren/zoekopdracht/actief",
      body,
    );
  }

  removeZoekopdrachtActief(
    werklijst: GeneratedType<"Werklijst">,
  ): Observable<void> {
    return this.http
      .delete<void>(`${this.basepath}/zoekopdracht/${werklijst}/actief`)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  readTabelGegevens(
    werklijst: GeneratedType<"Werklijst">,
  ): Observable<TabelGegevens> {
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
    );
  }

  updateDashboardCards(
    body: PutBody<"/rest/gebruikersvoorkeuren/dasboardcard/actief">,
  ) {
    return this.zacHttpClient.PUT(
      "/rest/gebruikersvoorkeuren/dasboardcard/actief",
      body,
    );
  }

  addDashboardCard(body: PutBody<"/rest/gebruikersvoorkeuren/dasboardcard">) {
    return this.zacHttpClient.PUT(
      "/rest/gebruikersvoorkeuren/dasboardcard",
      body,
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
