/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { inject, Injectable } from "@angular/core";
import { ActivatedRouteSnapshot } from "@angular/router";
import { QueryClient } from "@tanstack/angular-query-experimental";
import { ZakenService } from "./zaken.service";

@Injectable({
  providedIn: "root",
})
export class ZaakIdentificatieResolver {
  private readonly zakenService = inject(ZakenService);
  private readonly queryClient = inject(QueryClient);

  resolve(route: ActivatedRouteSnapshot) {
    const zaakID = route.paramMap.get("zaakIdentificatie");
    if (!zaakID) {
      throw new Error(
        `${ZaakIdentificatieResolver.name}: No 'zaakID' found in route`,
      );
    }

    return this.queryClient.ensureQueryData(
      this.zakenService.readZaakByID(zaakID),
    );
  }
}
