/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HttpErrorResponse } from "@angular/common/http";
import { inject, InjectionToken } from "@angular/core";
import { QueryCache, QueryClient } from "@tanstack/angular-query-experimental";
import {
  FoutAfhandelingService,
  isLoggedOut,
} from "../../fout-afhandeling/fout-afhandeling.service";

export type ZacMeta = {
  /**
   * `false` for a request that reports its own failure. The blocking dialog
   * then does not appear, and the caller is on the hook for telling the user
   * what went wrong. Defaults to reporting.
   */
  reportErrors?: boolean;
  /**
   * `true` for a read that the user is waiting on each time it runs, such as a
   * re-read after a save. A failed refetch of such a read opens the dialog,
   * even when a cached copy is still on screen.
   */
  reportFailedRefetch?: boolean;
};

declare module "@tanstack/angular-query-experimental" {
  interface Register {
    queryMeta: ZacMeta;
    mutationMeta: ZacMeta;
  }
}

/** @see ZacMeta.reportErrors */
export function reportsErrors(meta: ZacMeta | undefined) {
  return meta?.reportErrors !== false;
}

/**
 * A mutation reports its own failures through `onError`; a query has no such
 * option, so every read is reported from the cache instead. The cache reports
 * once a query has given up, not once per retry.
 *
 * Only a response is reported: anything else that fails a read, such as a
 * read that is cancelled, is not a failure the server gave.
 */
export const QUERY_CLIENT = new InjectionToken<QueryClient>("QUERY_CLIENT", {
  providedIn: "root",
  factory: () => {
    const foutAfhandelingService = inject(FoutAfhandelingService);

    return new QueryClient({
      queryCache: new QueryCache({
        onError: (error, query) => {
          if (!(error instanceof HttpErrorResponse)) return;
          if (!reportsErrors(query.meta)) return;

          if (
            query.state.data !== undefined &&
            !query.meta?.reportFailedRefetch &&
            !isLoggedOut(error)
          ) {
            console.error(error);
            return;
          }

          foutAfhandelingService.foutAfhandelen(error);
        },
      }),
    });
  },
});
