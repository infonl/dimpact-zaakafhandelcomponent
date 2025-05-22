/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { Observable } from "rxjs";
import { catchError } from "rxjs/operators";
import { FoutAfhandelingService } from "../fout-afhandeling/fout-afhandeling.service";
import { OverigeRechten } from "./model/overige-rechten";
import { WerklijstRechten } from "./model/werklijst-rechten";
import { GeneratedType } from "../shared/utils/generated-types";

@Injectable({
  providedIn: "root",
})
export class PolicyService {
  constructor(
    private http: HttpClient,
    private foutAfhandelingService: FoutAfhandelingService,
  ) {}

  private basepath = "/rest/policy";

  readWerklijstRechten(): Observable<WerklijstRechten> {
    return this.http
      .get<WerklijstRechten>(`${this.basepath}/werklijstRechten`)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  readOverigeRechten(): Observable<OverigeRechten> {
    return this.http
      .get<OverigeRechten>(`${this.basepath}/overigeRechten`)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  readNotitieRechten() {
    return this.http
      .get<
        GeneratedType<"RestNotitieRechten">
      >(`${this.basepath}/notitieRechten`)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }
}
