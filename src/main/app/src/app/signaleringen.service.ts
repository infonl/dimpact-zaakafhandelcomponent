/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { BehaviorSubject, Observable } from "rxjs";
import { catchError, map, switchMap } from "rxjs/operators";
import { WebsocketService } from "./core/websocket/websocket.service";
import { FoutAfhandelingService } from "./fout-afhandeling/fout-afhandeling.service";
import { EnkelvoudigInformatieobject } from "./informatie-objecten/model/enkelvoudig-informatieobject";
import { SignaleringType } from "./shared/signaleringen/signalering-type";
import { SignaleringTaakSummary } from "./signaleringen/model/signalering-taak-summary";
import { ZaakOverzichtDashboard } from "./zaken/model/zaak-overzicht-dashboard";
import { Resultaat } from "./shared/model/resultaat";

@Injectable({
  providedIn: "root",
})
export class SignaleringenService {
  private basepath = "/rest/signaleringen";

  private latestSignaleringSubject: BehaviorSubject<void> =
    new BehaviorSubject<void>(null);
  latestSignalering$ = this.latestSignaleringSubject.pipe(
    switchMap(() => this.http.get<string>(`${this.basepath}/latest`)),
  );

  constructor(
    private http: HttpClient,
    private foutAfhandelingService: FoutAfhandelingService,
    private websocketService: WebsocketService,
  ) {}

  updateSignaleringen(): void {
    this.latestSignaleringSubject.next();
  }

  listDashboardSignaleringTypen(): Observable<SignaleringType[]> {
    return this.http
      .get<SignaleringType[]>(`${this.basepath}/typen/dashboard`)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  listZakenSignalering(params: {
    signaleringType: SignaleringType;
    page: number;
    rows: number;
  }): Observable<Resultaat<ZaakOverzichtDashboard>> {
    const { signaleringType, page, rows } = params;
    return this.http
      .put<Resultaat<ZaakOverzichtDashboard>>(
        `${this.basepath}/zaken/${signaleringType}`,
        {
          page,
          rows,
        },
      )
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  listTakenSignalering(
    signaleringType: SignaleringType,
  ): Observable<SignaleringTaakSummary[]> {
    return this.http
      .get<
        SignaleringTaakSummary[]
      >(`${this.basepath}/taken/${signaleringType}`)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  listInformatieobjectenSignalering(
    signaleringType: SignaleringType,
  ): Observable<EnkelvoudigInformatieobject[]> {
    return this.http
      .get<
        EnkelvoudigInformatieobject[]
      >(`${this.basepath}/informatieobjecten/${signaleringType}`)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }
}
