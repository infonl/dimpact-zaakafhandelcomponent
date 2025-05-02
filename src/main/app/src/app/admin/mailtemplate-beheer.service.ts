/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { Observable } from "rxjs";
import { catchError } from "rxjs/operators";
import { FoutAfhandelingService } from "../fout-afhandeling/fout-afhandeling.service";
import { GeneratedType } from "../shared/utils/generated-types";
import { Mail } from "./model/mail";
import { Mailtemplate } from "./model/mailtemplate";
import { MailtemplateVariabele } from "./model/mailtemplate-variabele";

@Injectable({
  providedIn: "root",
})
export class MailtemplateBeheerService {
  private basepath = "/rest/beheer/mailtemplates";

  constructor(
    private http: HttpClient,
    private foutAfhandelingService: FoutAfhandelingService,
  ) {}

  readMailtemplate(id: string): Observable<Mailtemplate> {
    return this.http
      .get<Mailtemplate>(`${this.basepath}/${id}`)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  listMailtemplates(): Observable<Mailtemplate[]> {
    return this.http
      .get<Mailtemplate[]>(`${this.basepath}`)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  listKoppelbareMailtemplates() {
    return this.http
      .get<GeneratedType<"RESTMailtemplate">[]>(`${this.basepath}/koppelbaar`)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  deleteMailtemplate(id: number): Observable<void> {
    return this.http
      .delete<void>(`${this.basepath}/${id}`)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  persistMailtemplate(mailtemplate: Mailtemplate): Observable<Mailtemplate> {
    return this.http
      .put<Mailtemplate>(`${this.basepath}`, mailtemplate)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  ophalenVariabelenVoorMail(mail: Mail): Observable<MailtemplateVariabele[]> {
    return this.http
      .get<MailtemplateVariabele[]>(`${this.basepath}/variabelen/${mail}`)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }
}
