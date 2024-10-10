/*
 * SPDX-FileCopyrightText: 2024 Dimpact
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { Observable } from "rxjs";
import { catchError } from "rxjs/operators";
import { FoutAfhandelingService } from "../fout-afhandeling/fout-afhandeling.service";
import { FormioFormulier } from "./model/formio-formulier";
import { FormioFormulierContent } from "./model/formio-formulier-content";

@Injectable({
  providedIn: "root",
})
export class FormioFormulierenService {
  private basepath = "/rest/formioformulieren";

  constructor(
    private http: HttpClient,
    private foutAfhandelingService: FoutAfhandelingService,
  ) {}

  listFormioFormulieren(): Observable<FormioFormulier[]> {
    return this.http
      .get<FormioFormulier[]>(`${this.basepath}`)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  uploadFormioFormulier(
    formioFormulierContent: FormioFormulierContent,
  ): Observable<void> {
    return this.http
      .post<void>(`${this.basepath}`, formioFormulierContent)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  deleteFormioFormulier(formioFormulier: FormioFormulier): Observable<void> {
    return this.http
      .delete<void>(`${this.basepath}/${formioFormulier.id}`)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }
}
