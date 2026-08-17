/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  type CreateMutationOptions,
  type DefaultError,
  injectMutation as injectTanStackMutation,
} from "@tanstack/angular-query-experimental";
import { mergeMutationOptions } from "./merge-mutation-options";

/**
 * Drop-in replacement for the TanStack `injectMutation` that layers `overrides`
 * on top of the mutation options instead of replacing them.
 *
 * Passing the overrides as a second argument rather than spreading them into the
 * options keeps every callback of every layer: the client reports the failure,
 * the service invalidates its caches and the component updates its own state.
 */
export function injectMutation<
  TData = unknown,
  TError = DefaultError,
  TVariables = void,
  TOnMutateResult = unknown,
>(
  options: () => CreateMutationOptions<
    TData,
    TError,
    TVariables,
    TOnMutateResult
  >,
  overrides: Omit<
    CreateMutationOptions<TData, TError, TVariables, TOnMutateResult>,
    "mutationFn" | "mutationKey"
  > = {},
) {
  return injectTanStackMutation(() =>
    mergeMutationOptions(options(), overrides),
  );
}
