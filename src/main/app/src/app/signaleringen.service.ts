/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { BehaviorSubject, Observable } from "rxjs";
import { catchError, switchMap } from "rxjs/operators";
import { FoutAfhandelingService } from "./fout-afhandeling/fout-afhandeling.service";
import { Resultaat } from "./shared/model/resultaat";
import { GeneratedType } from "./shared/utils/generated-types";
import { ZaakOverzichtDashboard } from "./zaken/model/zaak-overzicht-dashboard";

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

  listDashboardSignaleringTypen() {
    return this.http
      .get<
        GeneratedType<"RestSignaleringInstellingen">["type"][]
      >(`${this.basepath}/typen/dashboard`)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  listZakenSignalering(params: {
    signaleringType: GeneratedType<"RestSignaleringInstellingen">["type"];
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
    signaleringType: GeneratedType<"RestSignaleringInstellingen">["type"],
  ) {
    return this.http
      .get<
        GeneratedType<"RestSignaleringTaskSummary">[]
      >(`${this.basepath}/taken/${signaleringType}`)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  listInformatieobjectenSignalering(
    signaleringType: GeneratedType<"RestSignaleringInstellingen">["type"],
  ) {
    return this.http
      .get<
        GeneratedType<"RestEnkelvoudigInformatieobject">[]
      >(`${this.basepath}/informatieobjecten/${signaleringType}`)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }
}
