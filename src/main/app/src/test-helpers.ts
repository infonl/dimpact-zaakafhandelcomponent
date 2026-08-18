/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 *
 */

import { OnChanges, SimpleChange, SimpleChanges } from "@angular/core";
import type {
  CreateMutationOptions,
  MutationFunctionContext,
} from "@tanstack/angular-query-experimental";

type DeepPartial<T> = T extends null | undefined
  ? T
  : T extends object
    ? { [P in keyof T]?: DeepPartial<T[P]> }
    : T;

export const fromPartial = <T,>(partial: NoInfer<DeepPartial<T>>): T =>
  partial as T;

export function updateComponentInputs<T extends OnChanges>(
  component: T,
  changes: Partial<T>,
  firstChange = false,
) {
  const simpleChanges: SimpleChanges = {};

  Object.keys(changes).forEach((changeKey) => {
    const typedKey = changeKey as keyof T;
    const value = changes[typedKey] as T[keyof T];
    component[typedKey] = value;
    simpleChanges[changeKey] = new SimpleChange(null, value, firstChange);
  });
  component.ngOnChanges(simpleChanges);
}

/**
 * Invokes the `onSuccess` a service attached to its mutation options, without
 * going through TanStack Query.
 */
export function runMutationOnSuccess<
  TData,
  TError,
  TVariables,
  TOnMutateResult,
>(
  options: CreateMutationOptions<TData, TError, TVariables, TOnMutateResult>,
  variables?: TVariables,
) {
  return options.onSuccess?.(
    undefined as TData,
    variables as TVariables,
    undefined as TOnMutateResult,
    fromPartial<MutationFunctionContext>({}),
  );
}

export function createMutationOptions<TData, TVariables = void>(data: TData) {
  const mutationFn = jest
    .fn<Promise<TData>, [TVariables]>()
    .mockResolvedValue(data);

  return {
    mutationKey: ["test-mutation"],
    mutationFn,
  };
}
