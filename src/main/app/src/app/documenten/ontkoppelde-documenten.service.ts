/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { Observable } from "rxjs";
import { catchError } from "rxjs/operators";
import { FoutAfhandelingService } from "../fout-afhandeling/fout-afhandeling.service";
import { ListParameters } from "../shared/model/list-parameters";
import { OntkoppeldDocument } from "./model/ontkoppeld-document";
import { OntkoppeldeDocumentenResultaat } from "./model/ontkoppelde-documenten-resultaat";

@Injectable({
  providedIn: "root",
})
export class OntkoppeldeDocumentenService {
  private basepath = "/rest/ontkoppeldedocumenten";

  constructor(
    private http: HttpClient,
    private foutAfhandelingService: FoutAfhandelingService,
  ) {}

  list(parameters: ListParameters): Observable<OntkoppeldeDocumentenResultaat> {
    return this.http
      .put<OntkoppeldeDocumentenResultaat>(this.basepath, parameters)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  delete(od: OntkoppeldDocument): Observable<void> {
    return this.http.delete<void>(`${this.basepath}/${od.id}`);
  }
}
