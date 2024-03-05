/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { Observable } from "rxjs";
import { catchError } from "rxjs/operators";
import { FoutAfhandelingService } from "../fout-afhandeling/fout-afhandeling.service";
import { MailGegevens } from "./model/mail-gegevens";

@Injectable({
  providedIn: "root",
})
export class MailService {
  private basepath = "/rest/mail";

  constructor(
    private http: HttpClient,
    private foutAfhandelingService: FoutAfhandelingService,
  ) {}

  sendMail(zaakUuid: string, mailGegevens: MailGegevens): Observable<any> {
    return this.http
      .post<number>(`${this.basepath}/send/${zaakUuid}`, mailGegevens)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  sendAcknowledgeReceipt(
    zaakUuid: string,
    mailGegevens: MailGegevens,
  ): Observable<any> {
    return this.http
      .post<number>(`${this.basepath}/acknowledge/${zaakUuid}`, mailGegevens)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }
}
