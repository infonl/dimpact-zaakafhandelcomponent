/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { catchError } from "rxjs/operators";
import { FoutAfhandelingService } from "../fout-afhandeling/fout-afhandeling.service";
import { GeneratedType } from "../shared/utils/generated-types";

@Injectable({
  providedIn: "root",
})
export class MailService {
  private basepath = "/rest/mail";

  constructor(
    private http: HttpClient,
    private foutAfhandelingService: FoutAfhandelingService,
  ) {}

  sendMail(zaakUuid: string, mailGegevens: GeneratedType<"RESTMailGegevens">) {
    return this.http
      .post<number>(`${this.basepath}/send/${zaakUuid}`, mailGegevens)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  sendAcknowledgeReceipt(
    zaakUuid: string,
    mailGegevens: GeneratedType<"RESTMailGegevens">,
  ) {
    return this.http
      .post<number>(`${this.basepath}/acknowledge/${zaakUuid}`, mailGegevens)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }
}
