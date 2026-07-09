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
import { Resultaat } from "../shared/model/resultaat";
import { InboxDocument } from "./model/inbox-document";

@Injectable({
  providedIn: "root",
})
export class InboxDocumentenService {
  private basepath = "/rest/inboxdocumenten";

  constructor(
    private http: HttpClient,
    private foutAfhandelingService: FoutAfhandelingService,
  ) {}

  list(parameters: ListParameters): Observable<Resultaat<InboxDocument>> {
    return this.http
      .put<Resultaat<InboxDocument>>(this.basepath, parameters)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  delete(od: InboxDocument): Observable<void> {
    return this.http.delete<void>(`${this.basepath}/${od.id}`);
  }
}
