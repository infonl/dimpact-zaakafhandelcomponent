/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HttpClient, HttpParams } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { Observable } from "rxjs";
import { catchError } from "rxjs/operators";
import { FoutAfhandelingService } from "../fout-afhandeling/fout-afhandeling.service";
import { TableRequest } from "../shared/dynamic-table/datasource/table-request";
import { TaakHistorieRegel } from "../shared/historie/model/taak-historie-regel";
import { PutBody, ZacHttpClient } from "../shared/http/zac-http-client";
import { GeneratedType } from "../shared/utils/generated-types";
import { TaakZoekObject } from "../zoeken/model/taken/taak-zoek-object";
import { Taak } from "./model/taak";
import { TaakToekennenGegevens } from "./model/taak-toekennen-gegevens";
import { TaakVerdelenGegevens } from "./model/taak-verdelen-gegevens";

@Injectable({
  providedIn: "root",
})
export class TakenService {
  constructor(
    private readonly http: HttpClient,
    private readonly foutAfhandelingService: FoutAfhandelingService,
    private readonly zacHttpClient: ZacHttpClient,
  ) {}

  private basepath = "/rest/taken";

  private static getTableParams(request: TableRequest): HttpParams {
    return new HttpParams().set("tableRequest", JSON.stringify(request));
  }

  readTaak(id: string): Observable<Taak> {
    return this.http
      .get<Taak>(`${this.basepath}/${id}`)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  listTakenVoorZaak(uuid: string): Observable<Taak[]> {
    return this.http
      .get<Taak[]>(`${this.basepath}/zaak/${uuid}`)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  listHistorieVoorTaak(id: string): Observable<TaakHistorieRegel[]> {
    return this.http
      .get<TaakHistorieRegel[]>(`${this.basepath}/${id}/historie`)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  toekennen(taak: Taak, reden: string): Observable<void> {
    const taakToekennenGegevens: TaakToekennenGegevens =
      new TaakToekennenGegevens();
    taakToekennenGegevens.taakId = taak.id;
    taakToekennenGegevens.zaakUuid = taak.zaakUuid;
    taakToekennenGegevens.groepId = taak.groep?.id;
    taakToekennenGegevens.behandelaarId = taak.behandelaar?.id;
    taakToekennenGegevens.reden = reden;

    return this.http
      .patch<void>(`${this.basepath}/toekennen`, taakToekennenGegevens)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  toekennenAanIngelogdeMedewerker(taak: Taak): Observable<Taak> {
    const taakToekennenGegevens: TaakToekennenGegevens =
      new TaakToekennenGegevens();
    taakToekennenGegevens.taakId = taak.id;
    taakToekennenGegevens.zaakUuid = taak.zaakUuid;
    return this.http
      .patch<Taak>(`${this.basepath}/toekennen/mij`, taakToekennenGegevens)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  toekennenAanIngelogdeMedewerkerVanuitLijst(
    taak: TaakZoekObject,
  ): Observable<Taak> {
    const taakToekennenGegevens: TaakToekennenGegevens =
      new TaakToekennenGegevens();
    taakToekennenGegevens.taakId = taak.id;
    taakToekennenGegevens.zaakUuid = taak.zaakUuid;
    return this.http
      .patch<Taak>(
        `${this.basepath}/lijst/toekennen/mij`,
        taakToekennenGegevens,
      )
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  update(taak: Taak): Observable<Taak> {
    return this.http
      .patch<Taak>(`${this.basepath}`, taak)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  updateTaakdata(taak: Taak): Observable<Taak> {
    return this.http
      .put<Taak>(`${this.basepath}/taakdata`, taak)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  complete(taak: Taak): Observable<Taak> {
    return this.http
      .patch<Taak>(`${this.basepath}/complete`, taak)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  verdelenVanuitLijst(
    taken: TaakZoekObject[],
    reden: string,
    screenEventResourceId: string,
    groep?: GeneratedType<"RestGroup">,
    medewerker?: GeneratedType<"RestUser">,
  ): Observable<void> {
    const taakBody: TaakVerdelenGegevens = new TaakVerdelenGegevens();
    taakBody.taken = taken.map((taak) => ({
      taakId: taak.id,
      zaakUuid: taak.zaakUuid,
    }));
    taakBody.behandelaarGebruikersnaam = medewerker?.id;
    taakBody.groepId = groep?.id;
    taakBody.reden = reden;
    taakBody.screenEventResourceId = screenEventResourceId;
    return this.http
      .put<void>(`${this.basepath}/lijst/verdelen`, taakBody)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  vrijgevenVanuitLijst(body: PutBody<"/rest/taken/lijst/vrijgeven">) {
    return this.zacHttpClient.PUT("/rest/taken/lijst/vrijgeven", body, {});
  }
}
