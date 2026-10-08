/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import type {
  CreateMutationOptions,
  DefaultError,
} from "@tanstack/angular-query-experimental";
import type { ZacMeta } from "./query-client";

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
 * Layers the `meta` of the overrides on top of that of the mutation, so that
 * setting one key does not drop the others. A key both layers define can only
 * take one value, so the outermost one wins and the caller is told which entry
 * it silently replaced.
 */
function mergeMeta(base: ZacMeta | undefined, overrides: ZacMeta | undefined) {
  if (!base || !overrides) return base ?? overrides;

  for (const [key, baseValue] of Object.entries(base)) {
    const overrideValue = overrides[key as keyof ZacMeta];
    if (!(key in overrides) || overrideValue === baseValue) continue;

    console.warn(
      `mergeMutationOptions: meta.${key} of the overrides replaces that of the mutation`,
      { mutation: baseValue, overrides: overrideValue },
    );
  }

  return { ...base, ...overrides };
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
  // `NoInfer` keeps `base` the only source of the type arguments, so the callbacks
  // of `overrides` are typed by the mutation they layer on instead of defining it
  overrides: Omit<
    CreateMutationOptions<
      NoInfer<TData>,
      NoInfer<TError>,
      NoInfer<TVariables>,
      NoInfer<TOnMutateResult>
    >,
    "mutationFn" | "mutationKey"
  >,
): CreateMutationOptions<TData, TError, TVariables, TOnMutateResult> {
  const baseOnMutate = base.onMutate;
  const overridesOnMutate = overrides.onMutate;

  return {
    ...base,
    ...overrides,
    meta: mergeMeta(base.meta, overrides.meta),
    onMutate:
      baseOnMutate && overridesOnMutate
        ? async (variables, context) => {
            const baseOnMutateResult = await baseOnMutate(variables, context);
            const overridesOnMutateResult = await overridesOnMutate(
              variables,
              context,
            );

            // there is no generic way to combine two contexts, so the outermost one wins
            if (
              baseOnMutateResult !== undefined &&
              overridesOnMutateResult !== undefined
            ) {
              console.warn(
                "mergeMutationOptions: the onMutate context of the overrides replaces that of the mutation",
                {
                  mutation: baseOnMutateResult,
                  overrides: overridesOnMutateResult,
                },
              );
            }

            return (overridesOnMutateResult ??
              baseOnMutateResult) as TOnMutateResult;
          }
        : (overridesOnMutate ?? baseOnMutate),
    onError: chain(base.onError, overrides.onError),
    onSuccess: chain(base.onSuccess, overrides.onSuccess),
    onSettled: chain(base.onSettled, overrides.onSettled),
  };
}
