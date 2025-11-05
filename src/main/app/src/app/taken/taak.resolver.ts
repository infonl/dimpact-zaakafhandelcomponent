/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { inject, Injectable } from "@angular/core";
import { ActivatedRouteSnapshot } from "@angular/router";
import { QueryClient } from "@tanstack/angular-query-experimental";
import { lastValueFrom } from "rxjs";
import { ZakenService } from "../zaken/zaken.service";
import { TakenService } from "./taken.service";

@Injectable({
  providedIn: "root",
})
export class TaakResolver {
  private readonly queryClient = inject(QueryClient);
  private readonly takenService = inject(TakenService);
  private readonly zakenService = inject(ZakenService);

  async resolve(route: ActivatedRouteSnapshot) {
    const taakID = route.paramMap.get("id");
    if (!taakID) {
      throw new Error(`${TaakResolver.name}: No 'id' found in route`);
    }

    const taak = await lastValueFrom(this.takenService.readTaak(taakID));
    void this.queryClient.prefetchQuery(
      this.zakenService.readZaak(taak.zaakUuid),
    );
    return taak;
  }
}
