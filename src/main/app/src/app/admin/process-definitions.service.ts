/*
 * SPDX-FileCopyrightText: 2024 Dimpact
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { Observable } from "rxjs";
import { catchError } from "rxjs/operators";
import { FoutAfhandelingService } from "../fout-afhandeling/fout-afhandeling.service";
import { ProcessDefinition } from "./model/process-definition";
import { ProcessDefinitionContent } from "./model/process-definition-content";

@Injectable({
  providedIn: "root",
})
export class ProcessDefinitionsService {
  private basepath = "/rest/processdefinitions";

  constructor(
    private http: HttpClient,
    private foutAfhandelingService: FoutAfhandelingService,
  ) {}

  listProcessDefinitions(): Observable<ProcessDefinition[]> {
    return this.http
      .get<ProcessDefinition[]>(`${this.basepath}`)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  uploadProcessDefinition(
    processDefinitionContent: ProcessDefinitionContent,
  ): Observable<void> {
    return this.http
      .post<void>(`${this.basepath}`, processDefinitionContent)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  deleteProcessDefinition(
    processDefinition: ProcessDefinition,
  ): Observable<void> {
    return this.http
      .delete<void>(`${this.basepath}/${processDefinition.key}`)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }
}
