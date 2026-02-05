/*
 * SPDX-FileCopyrightText: 2023 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { inject, Injectable } from "@angular/core";
import { ActivatedRouteSnapshot } from "@angular/router";
import { QueryClient } from "@tanstack/angular-query-experimental";
import { FoutAfhandelingService } from "../../fout-afhandeling/fout-afhandeling.service";
import { DEFAULT_RETRY_COUNT } from "../../shared/http/zac-query-client";
import { KlantenService } from "../klanten.service";

@Injectable({
  providedIn: "root",
})
export class PersoonResolverService {
  private readonly queryClient = inject(QueryClient);
  private readonly klantenService = inject(KlantenService);
  private readonly foutAfhandelingService = inject(FoutAfhandelingService);

  async resolve(route: ActivatedRouteSnapshot) {
    const temporaryPersonId = route.paramMap.get("temporaryPersonId")!;

    if (!temporaryPersonId) {
      throw new Error(
        `${PersoonResolverService.name}: no 'temporaryPersonId' found in route`,
      );
    }

    return this.queryClient.ensureQueryData({
      ...this.klantenService.readPersoon(temporaryPersonId),
      retry: (count, error) => {
        if (count < DEFAULT_RETRY_COUNT) return true;

        this.foutAfhandelingService.httpErrorAfhandelen(error);
        return false;
      },
    });
  }
}
