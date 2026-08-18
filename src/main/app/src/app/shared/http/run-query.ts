/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  type FetchQueryOptions,
  QueryClient,
} from "@tanstack/angular-query-experimental";
import { defer, Observable } from "rxjs";

/**
 * Reports a query as an observable, for the callers that are not components and
 * cannot use `injectQuery`.
 *
 * The observable is cold, so re-subscribing re-reads. Going through the query
 * client rather than calling `queryFn` directly means these callers share the
 * cache with the components, and are invalidated along with them.
 */
export function runQuery<TData, TError, TQueryKey extends readonly unknown[]>(
  queryClient: QueryClient,
  options: FetchQueryOptions<TData, TError, TData, TQueryKey>,
): Observable<TData> {
  return defer(() => queryClient.fetchQuery(options));
}
