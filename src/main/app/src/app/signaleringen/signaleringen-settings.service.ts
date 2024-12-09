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
export class SignaleringenSettingsService {
  private basepath = "/rest/signaleringen";

  constructor(
    private http: HttpClient,
    private foutAfhandelingService: FoutAfhandelingService,
  ) {}

  list() {
    return this.http
      .get<
        GeneratedType<"RestSignaleringInstellingen">[]
      >(`${this.basepath}/instellingen`)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  put(instellingen: GeneratedType<"RestSignaleringInstellingen">) {
    return this.http
      .put<void>(`${this.basepath}/instellingen`, instellingen)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }
}
