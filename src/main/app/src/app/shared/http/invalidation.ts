/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import type {
  InvalidateQueryFilters,
  QueryClient,
  QueryKey,
} from "@tanstack/angular-query-experimental";
import type { PathsWithMethod } from "openapi-typescript-helpers";
import type { Body, PathParameters, Paths, Response } from "./http-client";

export type WriteMethod = "post" | "put" | "patch" | "delete";

type ReadPath = PathsWithMethod<Paths, "get">;

type WritePath<Method extends WriteMethod> = PathsWithMethod<Paths, Method>;

/** The endpoints that read over `PUT`, see `ZacQueryClient.PUT_QUERY`. */
export type SearchPath = Extract<
  PathsWithMethod<Paths, "put">,
  | "/rest/inbox-productaanvragen"
  | "/rest/inboxdocumenten"
  | "/rest/informatieobjecten/informatieobjectenList"
  | "/rest/ontkoppeldedocumenten"
  | "/rest/signaleringen/zaken/{type}"
  | "/rest/zoeken/list"
>;

export type WriteEndpoint = {
  [Method in WriteMethod]: `${Uppercase<Method>} ${WritePath<Method> & string}`;
}[WriteMethod];

type MethodOf<Endpoint extends WriteEndpoint> =
  Endpoint extends `${infer Verb} ${string}`
    ? Extract<Lowercase<Verb>, WriteMethod>
    : never;

type PathOf<Endpoint extends WriteEndpoint> =
  Endpoint extends `${string} ${infer Path}`
    ? Extract<Path, WritePath<MethodOf<Endpoint>>>
    : never;

export type WriteRequest<
  Path extends WritePath<Method>,
  Method extends WriteMethod,
> = {
  parameters: PathParameters<Path, Method>;
  body: Body<Path, Method>;
};

export type WriteContext<
  Path extends WritePath<Method>,
  Method extends WriteMethod,
> = WriteRequest<Path, Method> & { response: Response<Path, Method> };

declare const readTarget: unique symbol;

/** Made by `read` and `search` only, so that every key is a path of the API. */
export type ReadTarget = QueryKey & { readonly [readTarget]: true };

type Targets<Context> = (context: Context) => readonly ReadTarget[];

type NoInvalidation = {
  none: string;
  refresh?: never;
  markStale?: never;
  afterFailureToo?: never;
  awaitRefresh?: never;
};

type AfterSuccess<Context> = {
  none?: never;
  afterFailureToo?: never;
  awaitRefresh?: never;
} & (
  | { refresh: Targets<Context>; markStale?: Targets<Context> }
  | { refresh?: never; markStale: Targets<Context> }
);

type AfterFailureToo<Context> = {
  none?: never;
  refresh: Targets<Context>;
  markStale?: never;
  afterFailureToo: true;
  awaitRefresh?: true;
};

export type Rule<Path extends WritePath<Method>, Method extends WriteMethod> =
  | NoInvalidation
  | AfterSuccess<WriteContext<Path, Method>>
  | AfterFailureToo<WriteRequest<Path, Method>>;

export type InvalidationMap = {
  readonly [Endpoint in WriteEndpoint]: Rule<
    PathOf<Endpoint>,
    MethodOf<Endpoint>
  >;
};

/** A read over `GET`; without parameters, every read of that path. */
export function read<Path extends ReadPath>(
  path: Path,
  parameters?: PathParameters<Path, "get">,
) {
  return (parameters ? [path, parameters] : [path]) as unknown as ReadTarget;
}

/** A read over `PUT`; without a body, every search on that path. */
export function search<Path extends SearchPath>(
  path: Path,
  body?: Partial<NonNullable<Body<Path, "put">>>,
) {
  return (body ? [path, body] : [path]) as unknown as ReadTarget;
}

export function applyRule<Context>(
  queryClient: QueryClient,
  rule: NoInvalidation | AfterSuccess<Context> | AfterFailureToo<Context>,
  context: Context,
) {
  const invalidate = (
    targets: readonly ReadTarget[] = [],
    filters: InvalidateQueryFilters = {},
  ) =>
    Promise.all(
      targets.map((queryKey) =>
        queryClient.invalidateQueries({ queryKey, ...filters }),
      ),
    );

  void invalidate(rule.markStale?.(context), { refetchType: "none" });
  const refreshed = invalidate(rule.refresh?.(context));
  if (rule.awaitRefresh) return refreshed;
}
