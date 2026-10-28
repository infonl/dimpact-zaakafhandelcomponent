/*
 * SPDX-FileCopyrightText: 2021 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { inject, Injectable } from "@angular/core";
import { ActivatedRouteSnapshot } from "@angular/router";
import { QueryClient } from "@tanstack/angular-query-experimental";
import { TakenService } from "./taken.service";

@Injectable({
  providedIn: "root",
})
export class TaakResolver {
  private readonly takenService = inject(TakenService);
  private readonly queryClient = inject(QueryClient);

  resolve(route: ActivatedRouteSnapshot) {
    const taakID = route.paramMap.get("id");
    if (!taakID) {
      throw new Error(`${TaakResolver.name}: No 'id' found in route`);
    }
    return this.queryClient.fetchQuery(this.takenService.readTaak(taakID));
  }
}
