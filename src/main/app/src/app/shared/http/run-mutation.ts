/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  type CreateMutationOptions,
  QueryClient,
} from "@tanstack/angular-query-experimental";
import { defer, from, Observable } from "rxjs";

/**
 * Runs a mutation outside of an injection context and reports it as an
 * observable, for the dialogs that take one.
 *
 * The observable is cold, so a confirm dialog can be handed the request before
 * the user has confirmed it. Going through the mutation cache rather than
 * calling `mutationFn` directly keeps the callbacks of every layer, so a service
 * still invalidates its caches and a failure is still reported.
 */
export function runMutation<TData, TError, TVariables, TOnMutateResult>(
  queryClient: QueryClient,
  options: CreateMutationOptions<TData, TError, TVariables, TOnMutateResult>,
  variables: TVariables,
): Observable<TData> {
  return defer(() =>
    from(
      queryClient
        .getMutationCache()
        .build(queryClient, options)
        .execute(variables),
    ),
  );
}
