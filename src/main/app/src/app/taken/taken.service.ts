/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { Observable } from "rxjs";
import { catchError } from "rxjs/operators";
import { FoutAfhandelingService } from "../fout-afhandeling/fout-afhandeling.service";
import { TaakHistorieRegel } from "../shared/historie/model/taak-historie-regel";
import {
  PatchBody,
  PutBody,
  ZacHttpClient,
} from "../shared/http/zac-http-client";
import { GeneratedType } from "../shared/utils/generated-types";
import { TaakZoekObject } from "../zoeken/model/taken/taak-zoek-object";
import { Taak } from "./model/taak";
import { TaakToekennenGegevens } from "./model/taak-toekennen-gegevens";

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

  readTaak(taskId: string) {
    return this.zacHttpClient.GET("/rest/taken/{taskId}", {
      path: { taskId },
    });
  }

  listTakenVoorZaak(zaakUUID: string) {
    return this.zacHttpClient.GET("/rest/taken/zaak/{zaakUUID}", {
      path: { zaakUUID },
    });
  }

  listHistorieVoorTaak(id: string): Observable<TaakHistorieRegel[]> {
    return this.http
      .get<TaakHistorieRegel[]>(`${this.basepath}/${id}/historie`)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  toekennen(body: PatchBody<"/rest/taken/toekennen">) {
    return this.zacHttpClient.PATCH("/rest/taken/toekennen", body, {});
  }

  toekennenAanIngelogdeMedewerker(
    taak: GeneratedType<"RestTask">,
  ): Observable<Taak> {
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

  updateTaakdata(body: PutBody<"/rest/taken/taakdata">) {
    return this.zacHttpClient.PUT("/rest/taken/taakdata", body, {});
  }

  complete(body: PatchBody<"/rest/taken/complete">) {
    return this.zacHttpClient.PATCH("/rest/taken/complete", body, {});
  }

  verdelenVanuitLijst(
    body: PutBody<"/rest/taken/lijst/verdelen">
  ) {
    return this.zacHttpClient.PUT("/rest/taken/lijst/verdelen", body, {})
  }

  vrijgevenVanuitLijst(body: PutBody<"/rest/taken/lijst/vrijgeven">) {
    return this.zacHttpClient.PUT("/rest/taken/lijst/vrijgeven", body, {});
  }
}
