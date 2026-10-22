/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Injectable } from "@angular/core";
import { ActivatedRouteSnapshot } from "@angular/router";
import { TakenService } from "./taken.service";

@Injectable({
  providedIn: "root",
})
export class TaakResolver {
  constructor(private takenService: TakenService) {}

  resolve(route: ActivatedRouteSnapshot) {
    const taakID = route.paramMap.get("id");
    if (!taakID) {
      throw new Error(`${TaakResolver.name}: No 'id' found in route`);
    }
    return this.takenService.readTaak(taakID);
  }
}
