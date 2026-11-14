/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { Observable, catchError } from "rxjs";
import { FoutAfhandelingService } from "src/app/fout-afhandeling/fout-afhandeling.service";

@Injectable({
  providedIn: "root",
})
export class IndexingService {
  private basepath = "/rest/indexeren";

  constructor(
    private http: HttpClient,
    private foutAfhandelingService: FoutAfhandelingService,
  ) {}

  commitPendingChangesToSearchIndex(): Observable<void> {
    return this.http
      .post<void>(
        `${this.basepath}/commit-pending-changes-to-search-index`,
        null,
      )
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }
}
