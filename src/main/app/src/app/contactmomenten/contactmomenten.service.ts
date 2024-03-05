/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { Observable } from "rxjs";
import { catchError } from "rxjs/operators";
import { FoutAfhandelingService } from "../fout-afhandeling/fout-afhandeling.service";
import { Resultaat } from "../shared/model/resultaat";
import { Contactmoment } from "./model/contactmoment";
import { ListContactmomentenParameters } from "./model/list-contactmomenten-parameters";

@Injectable({
  providedIn: "root",
})
export class ContactmomentenService {
  constructor(
    private http: HttpClient,
    private foutAfhandelingService: FoutAfhandelingService,
  ) {}

  private basepath = "/rest/contactmomenten";

  listContactmomenten(
    listContactmomentenParameters: ListContactmomentenParameters,
  ): Observable<Resultaat<Contactmoment>> {
    return this.http
      .put<
        Resultaat<Contactmoment>
      >(`${this.basepath}`, listContactmomentenParameters)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }
}
