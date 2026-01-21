/*
 * SPDX-FileCopyrightText: 2023 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { inject, Injectable } from "@angular/core";
import { Router } from "@angular/router";
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
  private readonly router = inject(Router);

  async resolve() {
    const bsn = this.router.getCurrentNavigation()?.extras.state?.bsn;

    if (!bsn) {
      console.error(`${PersoonResolverService.name}: no 'BSN' provided`);
      return;
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
