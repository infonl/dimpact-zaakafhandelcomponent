/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { BehaviorSubject, Observable } from "rxjs";
import { catchError, switchMap } from "rxjs/operators";
import { FoutAfhandelingService } from "./fout-afhandeling/fout-afhandeling.service";
import { EnkelvoudigInformatieobject } from "./informatie-objecten/model/enkelvoudig-informatieobject";
import { SignaleringType } from "./shared/signaleringen/signalering-type";
import { Taak } from "./taken/model/taak";
import { ZaakOverzicht } from "./zaken/model/zaak-overzicht";

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

  listZakenSignalering(
    signaleringType: SignaleringType,
  ): Observable<ZaakOverzicht[]> {
    return this.http
      .get<ZaakOverzicht[]>(`${this.basepath}/zaken/${signaleringType}`)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  listTakenSignalering(signaleringType: SignaleringType): Observable<Taak[]> {
    return this.http
      .get<Taak[]>(`${this.basepath}/taken/${signaleringType}`)
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
