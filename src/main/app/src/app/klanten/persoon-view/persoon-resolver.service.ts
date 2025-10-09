/*
 * SPDX-FileCopyrightText: 2023 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { inject, Injectable } from "@angular/core";
import { ActivatedRouteSnapshot } from "@angular/router";
import { QueryClient } from "@tanstack/angular-query-experimental";
import { FoutAfhandelingService } from "../../fout-afhandeling/fout-afhandeling.service";
import { KlantenService } from "../klanten.service";
import {DEFAULT_RETRY_COUNT} from "../../shared/http/zac-query-client";

@Injectable({
  providedIn: "root",
})
export class PersoonResolverService {
  private readonly queryClient = inject(QueryClient);
  private readonly klantenService = inject(KlantenService);
  private readonly foutAfhandelingService = inject(FoutAfhandelingService);

  async resolve(route: ActivatedRouteSnapshot) {
    const bsn = route.paramMap.get("bsn");

    if (!bsn) {
      throw new Error(
        `${PersoonResolverService.name}: no 'bsn' found in route`,
      );
    }

    return this.queryClient.ensureQueryData({
      ...this.klantenService.readPersoon(bsn),
      retry: (count, error) => {
        if (count < DEFAULT_RETRY_COUNT) return true;

        this.foutAfhandelingService.httpErrorAfhandelen(error);
        return false;
      },
    });
  }
}
