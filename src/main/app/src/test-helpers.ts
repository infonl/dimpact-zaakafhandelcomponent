/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 *
 */

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
  data?: TData,
) {
  return options.onSuccess?.(
    data as TData,
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

export function createQueryOptions<TData>(data: TData) {
  const queryFn = jest.fn<Promise<TData>, []>().mockResolvedValue(data);

  return {
    queryKey: ["test-query"],
    queryFn,
  };
}

export const flushMicrotasks = () => Promise.resolve();
