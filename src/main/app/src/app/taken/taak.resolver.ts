/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Injectable } from "@angular/core";
import { ActivatedRouteSnapshot } from "@angular/router";
import { Observable } from "rxjs";
import { Taak } from "./model/taak";
import { TakenService } from "./taken.service";

@Injectable({
  providedIn: "root",
})
export class TaakResolver {
  constructor(private takenService: TakenService) {}

  resolve(route: ActivatedRouteSnapshot): Observable<Taak> {
    const taakID = route.paramMap.get("id");
    if (!taakID) {
      throw new Error(`${TaakResolver.name}: No 'id' found in route`);
    }
    return this.takenService.readTaak(taakID);
  }
}
