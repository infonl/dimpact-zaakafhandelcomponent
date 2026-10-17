/*
 * SPDX-FileCopyrightText: 2022 Atos, 20242 Lifely
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
export class SignaleringenSettingsBeheerService {
  private basepath = "/rest/signaleringen";

  constructor(
    private http: HttpClient,
    private foutAfhandelingService: FoutAfhandelingService,
  ) {}

  list(groupId: string) {
    return this.http
      .get<
        GeneratedType<"RestSignaleringInstellingen">[]
      >(`${this.basepath}/group/${groupId}/instellingen`)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  put(
    groupId: string,
    instellingen: GeneratedType<"RestSignaleringInstellingen">,
  ) {
    return this.http
      .put<void>(`${this.basepath}/group/${groupId}/instellingen`, instellingen)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }
}
