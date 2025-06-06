/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { Observable } from "rxjs";
import { catchError } from "rxjs/operators";
import { Mailtemplate } from "../admin/model/mailtemplate";
import { FoutAfhandelingService } from "../fout-afhandeling/fout-afhandeling.service";

@Injectable({
  providedIn: "root",
})
export class MailtemplateService {
  private basepath = "/rest/mailtemplates";

  constructor(
    private http: HttpClient,
    private foutAfhandelingService: FoutAfhandelingService,
  ) {}

  findMailtemplate(
    mailtemplateEnum: string,
    zaakUUID: string,
  ): Observable<Mailtemplate> {
    return this.http
      .get<Mailtemplate>(`${this.basepath}/${mailtemplateEnum}/${zaakUUID}`)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }
}
