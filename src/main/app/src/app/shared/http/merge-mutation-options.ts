/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import type {
  CreateMutationOptions,
  DefaultError,
} from "@tanstack/angular-query-experimental";

/**
 * Runs `first` and then `second`, or whichever of the two is defined. Returns
 * `undefined` when neither is, so that a callback nothing subscribed to does not
 * become an extra `await` in front of the mutation.
 */
function chain<TArguments extends unknown[]>(
  first: ((...args: TArguments) => unknown) | undefined,
  second: ((...args: TArguments) => unknown) | undefined,
) {
  if (!first || !second) return first ?? second;

  return async (...args: TArguments) => {
    await first(...args);
    await second(...args);
  };
}

/**
 * Layers `overrides` on top of `base` so that every mutation callback of both is
 * invoked, base first. Object spread would make the callbacks of `overrides`
 * replace those of `base`, which silently drops the error handling that
 * {@link ZacQueryClient} attaches to every mutation.
 */
export function mergeMutationOptions<
  TData = unknown,
  TError = DefaultError,
  TVariables = void,
  TOnMutateResult = unknown,
>(
  base: CreateMutationOptions<TData, TError, TVariables, TOnMutateResult>,
  overrides: Omit<
    CreateMutationOptions<TData, TError, TVariables, TOnMutateResult>,
    "mutationFn" | "mutationKey"
  >,
): CreateMutationOptions<TData, TError, TVariables, TOnMutateResult> {
  const baseOnMutate = base.onMutate;
  const overridesOnMutate = overrides.onMutate;

  return {
    ...base,
    ...overrides,
    onMutate:
      baseOnMutate && overridesOnMutate
        ? async (variables, context) => {
            const baseOnMutateResult = await baseOnMutate(variables, context);
            const overridesOnMutateResult = await overridesOnMutate(
              variables,
              context,
            );

            // there is no generic way to combine two contexts, so the outermost one wins
            return (overridesOnMutateResult ??
              baseOnMutateResult) as TOnMutateResult;
          }
        : (overridesOnMutate ?? baseOnMutate),
    onError: chain(base.onError, overrides.onError),
    onSuccess: chain(base.onSuccess, overrides.onSuccess),
    onSettled: chain(base.onSettled, overrides.onSettled),
  };
}
