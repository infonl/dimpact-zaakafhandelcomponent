/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { Observable } from "rxjs";
import { catchError } from "rxjs/operators";
import { FoutAfhandelingService } from "../fout-afhandeling/fout-afhandeling.service";
import { HumanTaskData } from "./model/human-task-data";
import { PlanItem } from "./model/plan-item";
import { ProcessTaskData } from "./model/process-task-data";
import { UserEventListenerData } from "./model/user-event-listener-data";

@Injectable({
  providedIn: "root",
})
export class PlanItemsService {
  private basepath = "/rest/planitems";

  constructor(
    private http: HttpClient,
    private foutAfhandelingService: FoutAfhandelingService,
  ) {}

  readHumanTaskPlanItem(planItemId: string): Observable<PlanItem> {
    return this.http
      .get<PlanItem>(`${this.basepath}/humanTaskPlanItem/${planItemId}`)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  readProcessTaskPlanItem(planItemId: string): Observable<PlanItem> {
    return this.http
      .get<PlanItem>(`${this.basepath}/processTaskPlanItem/${planItemId}`)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  listHumanTaskPlanItems(zaakUuid: string): Observable<PlanItem[]> {
    return this.http
      .get<PlanItem[]>(`${this.basepath}/zaak/${zaakUuid}/humanTaskPlanItems`)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  listProcessTaskPlanItems(zaakUuid: string): Observable<PlanItem[]> {
    return this.http
      .get<PlanItem[]>(`${this.basepath}/zaak/${zaakUuid}/processTaskPlanItems`)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  listUserEventListenerPlanItems(zaakUuid: string): Observable<PlanItem[]> {
    return this.http
      .get<
        PlanItem[]
      >(`${this.basepath}/zaak/${zaakUuid}/userEventListenerPlanItems`)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  doHumanTaskPlanItem(humanTaskData: HumanTaskData): Observable<void> {
    if (!humanTaskData.medewerker?.id) {
      humanTaskData.medewerker = null;
    }

    return this.http
      .post<void>(`${this.basepath}/doHumanTaskPlanItem`, humanTaskData)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  doProcessTaskPlanItem(processTaskData: ProcessTaskData): Observable<void> {
    return this.http
      .post<void>(`${this.basepath}/doProcessTaskPlanItem`, processTaskData)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  doUserEventListenerPlanItem(
    userEventListenerData: UserEventListenerData,
  ): Observable<void> {
    return this.http
      .post<void>(
        `${this.basepath}/doUserEventListenerPlanItem`,
        userEventListenerData,
      )
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }
}
