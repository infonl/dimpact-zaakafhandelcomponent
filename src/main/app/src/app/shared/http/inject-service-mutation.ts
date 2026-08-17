/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  type CreateMutationOptions,
  type CreateMutationResult,
  injectMutation,
} from "@tanstack/angular-query-experimental";

type ServiceMutationOptions<TData, TError, TVariables, TOnMutateResult> =
  CreateMutationOptions<TData, TError, TVariables, TOnMutateResult>;

type MutationContext = Parameters<
  NonNullable<
    CreateMutationOptions<unknown, unknown, unknown, unknown>["mutationFn"]
  >
>[1];

type InjectedMutationOptions<TData, TError, TVariables, TOnMutateResult> = Omit<
  CreateMutationOptions<TData, TError, TVariables, TOnMutateResult>,
  "mutationFn" | "mutationKey"
>;

type ServiceMutationFactory<
  TData,
  TError,
  TServiceVariables,
  TMutationVariables,
  TOnMutateResult,
> = (
  variables: TServiceVariables,
) => ServiceMutationOptions<TData, TError, TMutationVariables, TOnMutateResult>;

type ConfigurableServiceMutation<
  TData,
  TError,
  TServiceVariables,
  TMutationVariables,
  TOnMutateResult,
> = InjectedMutationOptions<
  TData,
  TError,
  TServiceVariables,
  TOnMutateResult
> & {
  mutationOptions: ServiceMutationFactory<
    TData,
    TError,
    TServiceVariables,
    TMutationVariables,
    TOnMutateResult
  >;
  body: (variables: TServiceVariables) => TMutationVariables;
};

function withServiceContext<TData, TError, TVariables, TOnMutateResult>(
  serviceMutationOptions: ServiceMutationOptions<
    TData,
    TError,
    TVariables,
    TOnMutateResult
  >,
  context: MutationContext,
): MutationContext {
  return {
    client: context.client,
    meta: serviceMutationOptions.meta,
    mutationKey: serviceMutationOptions.mutationKey,
  };
}

/**
 * Injects a mutation whose options depend on the variables it is called with.
 *
 * The two layers see different variables, because that mapping is the point of
 * this helper: the callbacks passed here receive what the component handed to
 * `mutate`, while the callbacks the service put on its own options receive the
 * request body that `body` derived from it. For a request without a body — a
 * `DELETE` addressed by path — the service side is therefore `undefined`, and a
 * service that needs the item it acted on takes it as a parameter and closes
 * over it.
 */
export function injectServiceMutation<
  TData,
  TError,
  TServiceVariables = void,
  TMutationVariables = unknown,
  TOnMutateResult = unknown,
>(
  mutationOptions: ServiceMutationFactory<
    TData,
    TError,
    TServiceVariables,
    TMutationVariables,
    TOnMutateResult
  >,
  options?: InjectedMutationOptions<
    TData,
    TError,
    TServiceVariables,
    TOnMutateResult
  >,
): CreateMutationResult<TData, TError, TServiceVariables, TOnMutateResult>;
export function injectServiceMutation<
  TData,
  TError,
  TServiceVariables,
  TMutationVariables,
  TOnMutateResult = unknown,
>(
  config: ConfigurableServiceMutation<
    TData,
    TError,
    TServiceVariables,
    TMutationVariables,
    TOnMutateResult
  >,
): CreateMutationResult<TData, TError, TServiceVariables, TOnMutateResult>;
export function injectServiceMutation<
  TData,
  TError,
  TServiceVariables,
  TMutationVariables,
  TOnMutateResult,
>(
  mutationOptionsOrConfig:
    | ServiceMutationFactory<
        TData,
        TError,
        TServiceVariables,
        TMutationVariables,
        TOnMutateResult
      >
    | ConfigurableServiceMutation<
        TData,
        TError,
        TServiceVariables,
        TMutationVariables,
        TOnMutateResult
      >,
  options?: InjectedMutationOptions<
    TData,
    TError,
    TServiceVariables,
    TOnMutateResult
  >,
) {
  const mutationOptions =
    typeof mutationOptionsOrConfig === "function"
      ? mutationOptionsOrConfig
      : mutationOptionsOrConfig.mutationOptions;
  const body =
    typeof mutationOptionsOrConfig === "function"
      ? (_variables: TServiceVariables) => undefined as TMutationVariables
      : mutationOptionsOrConfig.body;
  const overrides =
    typeof mutationOptionsOrConfig === "function"
      ? options
      : mutationOptionsOrConfig;

  return injectMutation(() => ({
    ...overrides,
    mutationFn: (variables: TServiceVariables, context: MutationContext) => {
      const serviceMutationOptions = mutationOptions(variables);

      return serviceMutationOptions.mutationFn!(
        body(variables),
        withServiceContext(serviceMutationOptions, context),
      );
    },
    onMutate: async (
      variables: TServiceVariables,
      context: MutationContext,
    ) => {
      const serviceMutationOptions = mutationOptions(variables);
      const serviceOnMutateResult = await serviceMutationOptions.onMutate?.(
        body(variables),
        withServiceContext(serviceMutationOptions, context),
      );
      const onMutateResult = await overrides?.onMutate?.(variables, context);

      // there is no generic way to combine two contexts, so the outermost one wins
      return (onMutateResult ?? serviceOnMutateResult) as TOnMutateResult;
    },
    onError: async (error, variables, onMutateResult, context) => {
      const serviceMutationOptions = mutationOptions(variables);

      await serviceMutationOptions.onError?.(
        error,
        body(variables),
        onMutateResult,
        context,
      );
      await overrides?.onError?.(error, variables, onMutateResult, context);
    },
    onSuccess: async (data, variables, onMutateResult, context) => {
      const serviceMutationOptions = mutationOptions(variables);

      await serviceMutationOptions.onSuccess?.(
        data,
        body(variables),
        onMutateResult,
        context,
      );
      await overrides?.onSuccess?.(data, variables, onMutateResult, context);
    },
    onSettled: async (data, error, variables, onMutateResult, context) => {
      const serviceMutationOptions = mutationOptions(variables);

      await serviceMutationOptions.onSettled?.(
        data,
        error,
        body(variables),
        onMutateResult,
        context,
      );
      await overrides?.onSettled?.(
        data,
        error,
        variables,
        onMutateResult,
        context,
      );
    },
  }));
}
