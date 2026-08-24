/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HttpErrorResponse } from "@angular/common/http";
import { inject, InjectionToken } from "@angular/core";
import { QueryCache, QueryClient } from "@tanstack/angular-query-experimental";
import { FoutAfhandelingService } from "../../fout-afhandeling/fout-afhandeling.service";

/**
 * A mutation reports its own failures through `onError`; a query has no such
 * option, so every read is reported from the cache instead. The cache reports
 * once a query has given up, not once per retry.
 */
export const QUERY_CLIENT = new InjectionToken<QueryClient>("QUERY_CLIENT", {
  providedIn: "root",
  factory: () => {
    const foutAfhandelingService = inject(FoutAfhandelingService);

    return new QueryClient({
      queryCache: new QueryCache({
        onError: (error) =>
          foutAfhandelingService.foutAfhandelen(error as HttpErrorResponse),
      }),
    });
  },
});
