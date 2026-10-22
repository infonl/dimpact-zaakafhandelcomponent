/*
 * SPDX-FileCopyrightText: 2021 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Injectable, inject } from "@angular/core";
import { ActivatedRouteSnapshot } from "@angular/router";
import { TakenService } from "./taken.service";

@Injectable({
  providedIn: "root",
})
export class TaakResolver {
  private takenService = inject(TakenService);

  resolve(route: ActivatedRouteSnapshot) {
    const taakID = route.paramMap.get("id");
    if (!taakID) {
      throw new Error(`${TaakResolver.name}: No 'id' found in route`);
    }
    return this.takenService.readTaak(taakID);
  }
}
