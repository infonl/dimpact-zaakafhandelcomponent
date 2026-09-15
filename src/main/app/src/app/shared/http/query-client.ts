/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HttpErrorResponse } from "@angular/common/http";
import { inject, InjectionToken } from "@angular/core";
import { QueryCache, QueryClient } from "@tanstack/angular-query-experimental";
import { FoutAfhandelingService } from "../../fout-afhandeling/fout-afhandeling.service";
import { HttpParamsError } from "./http-client";

export type ZacMeta = {
  /**
   * `false` for a request that reports its own failure. Neither the blocking
   * dialog nor the snackbar then appears, and the caller is on the hook for
   * telling the user what went wrong. Defaults to reporting.
   */
  reportErrors?: boolean;
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
 * A read that still goes through `ZacHttpClient` reports its own failure and
 * then rethrows the message it built, so what reaches the cache is a string
 * rather than a response. Reporting that too would close the dialog it just
 * opened and replace it with a generic one, so only a response is reported.
 */
export const QUERY_CLIENT = new InjectionToken<QueryClient>("QUERY_CLIENT", {
  providedIn: "root",
  factory: () => {
    const foutAfhandelingService = inject(FoutAfhandelingService);

    return new QueryClient({
      queryCache: new QueryCache({
        onError: (error, query) => {
          if (
            !(error instanceof HttpErrorResponse) &&
            !(error instanceof HttpParamsError)
          ) {
            return;
          }
          if (!reportsErrors(query.meta)) return;

          if (query.state.data !== undefined) {
            foutAfhandelingService.log("msg.error.verversen-mislukt")(error);
            return;
          }

          foutAfhandelingService.foutAfhandelen(error);
        },
      }),
    });
  },
});
