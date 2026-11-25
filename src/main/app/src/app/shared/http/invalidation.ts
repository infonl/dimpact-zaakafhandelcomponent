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

export type WriteEndpoint = {
  [Method in WriteMethod]: `${Uppercase<Method>} ${PathsWithMethod<Paths, Method> & string}`;
}[WriteMethod];

type MethodOf<Endpoint extends WriteEndpoint> =
  Endpoint extends `${infer Verb} ${string}`
    ? Extract<Lowercase<Verb>, WriteMethod>
    : never;

type PathOf<Endpoint extends WriteEndpoint> =
  Endpoint extends `${string} ${infer Path}`
    ? Extract<Path, PathsWithMethod<Paths, MethodOf<Endpoint>>>
    : never;

/** What a rule can read ids from. */
export type WriteContext<Endpoint extends WriteEndpoint> = {
  parameters: PathParameters<PathOf<Endpoint>, MethodOf<Endpoint>>;
  body: Body<PathOf<Endpoint>, MethodOf<Endpoint>>;
  response: Response<PathOf<Endpoint>, MethodOf<Endpoint>>;
};

declare const readTarget: unique symbol;

/** Made by `read` and `search` only, so that every key is a path of the API. */
export type ReadTarget = QueryKey & { readonly [readTarget]: true };

type ReadPath = PathsWithMethod<Paths, "get">;

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

type Targets<Context> = (context: Context) => readonly ReadTarget[];

export type Rule<Endpoint extends WriteEndpoint> =
  | {
      none: string;
      refresh?: never;
      markStale?: never;
      afterFailureToo?: never;
      awaitRefresh?: never;
    }
  | {
      none?: never;
      afterFailureToo?: never;
      awaitRefresh?: never;
      /** Reads that are on screen are read again at once. */
      refresh?: Targets<WriteContext<Endpoint>>;
      /** Read again on the next visit, for data that the write does not change at once, such as Solr. */
      markStale?: Targets<WriteContext<Endpoint>>;
    }
  | {
      /** Also after a failure, so a rule cannot rely on a response. */
      afterFailureToo: true;
      none?: never;
      markStale?: never;
      /** Keeps the write pending until what is on screen has been read again. */
      awaitRefresh?: true;
      refresh: Targets<Omit<WriteContext<Endpoint>, "response">>;
    };

export type InvalidationMap = {
  readonly [Endpoint in WriteEndpoint]: Rule<Endpoint>;
};

type AnyRule = {
  none?: string;
  afterFailureToo?: true;
  awaitRefresh?: true;
  refresh?: Targets<unknown>;
  markStale?: Targets<unknown>;
};

type Request = { parameters: unknown; body: unknown };

export const alsoAfterFailure = (rule: unknown) =>
  (rule as AnyRule).afterFailureToo === true;

export function applyRule(
  queryClient: QueryClient,
  rule: unknown,
  context: Request & { response?: unknown },
) {
  const { refresh, markStale, awaitRefresh } = rule as AnyRule;

  const invalidate = (
    targets: readonly ReadTarget[] = [],
    filters: InvalidateQueryFilters = {},
  ) =>
    Promise.all(
      targets.map((queryKey) =>
        queryClient.invalidateQueries({ queryKey, ...filters }),
      ),
    );

  void invalidate(markStale?.(context), { refetchType: "none" });
  const refreshed = invalidate(refresh?.(context));
  if (awaitRefresh) return refreshed;
}
