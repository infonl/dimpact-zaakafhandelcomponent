/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { inject, Injectable } from "@angular/core";
import { ActivatedRouteSnapshot } from "@angular/router";
import { QueryClient } from "@tanstack/angular-query-experimental";
import { ZakenService } from "./zaken.service";

@Injectable({
  providedIn: "root",
})
export class ZaakUuidResolver {
  private readonly zakenService = inject(ZakenService);
  private readonly queryClient = inject(QueryClient);

  async resolve(route: ActivatedRouteSnapshot) {
    const zaakUuid = route.paramMap.get("zaakUuid");
    if (!zaakUuid) {
      throw new Error("'zaakUuid' is missing in the route parameters");
    }

    await this.queryClient.invalidateQueries({
      queryKey: this.zakenService.readZaak(zaakUuid).queryKey,
    });

    return this.queryClient.ensureQueryData(
      this.zakenService.readZaak(zaakUuid),
    );
  }
}
