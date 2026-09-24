/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { inject, provideAppInitializer } from "@angular/core";
import { QueryClient } from "@tanstack/angular-query-experimental";
import { ConfiguratieService } from "../configuratie/configuratie.service";

export function provideStartupPrefetch() {
  return provideAppInitializer(() => {
    const queryClient = inject(QueryClient);
    const configuratieService = inject(ConfiguratieService);

    const startupQueries = [configuratieService.readAllowedFileTypesQuery()];

    // not awaited: the application must not wait for data it only needs later
    startupQueries.forEach((query) => void queryClient.prefetchQuery(query));
  });
}
