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

  async resolve(route: ActivatedRouteSnapshot) {
    const zaakIdentificatie = route.paramMap.get("zaakIdentificatie");
    if (!zaakIdentificatie) {
      throw new Error(
        `${ZaakIdentificatieResolver.name}: No 'zaakIdentificatie' found in route`,
      );
    }

    // We only use `ZakenService.readZaakByID` to map the `zaakIdentificatie` to a `uuid`.
    // We use the `ZakenService.readZaak` for all other queries
    const { uuid } = await this.queryClient.ensureQueryData(
      this.zakenService.readZaakByID(zaakIdentificatie),
    );

    const readZaakQueryOptions = this.zakenService.readZaak(uuid);

    await this.queryClient.refetchQueries({
      queryKey: readZaakQueryOptions.queryKey,
    });

    return this.queryClient.ensureQueryData(readZaakQueryOptions);
  }
}
