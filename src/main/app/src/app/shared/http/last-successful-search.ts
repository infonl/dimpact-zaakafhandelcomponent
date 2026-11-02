/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HttpErrorResponse } from "@angular/common/http";
import {
  computed,
  effect,
  inject,
  linkedSignal,
  untracked,
  WritableSignal,
} from "@angular/core";
import {
  CreateQueryOptions,
  hashKey,
  injectQuery,
  QueryKey,
} from "@tanstack/angular-query-experimental";
import {
  FoutAfhandelingOptions,
  FoutAfhandelingService,
  isLoggedOut,
} from "../../fout-afhandeling/fout-afhandeling.service";

export type ShownSearch<TParams, TData> = {
  params: TParams;
  data: TData;
};

export type SearchQueryOptions<TData> = Omit<
  CreateQueryOptions<TData, HttpErrorResponse, TData, QueryKey>,
  "select" | "meta"
>;

/**
 * Shows the last search that succeeded, together with the parameters it was
 * made with, and steps back to it when a search fails.
 *
 * `requested` is what the user asks for; it is the query key. A failed search
 * puts `requested` back on the parameters of the shown search, so the filters,
 * the sort and the paginator that are bound to it follow the rows on screen.
 */
export function injectLastSuccessfulSearch<TParams, TData>({
  requested,
  query,
  onRevert,
}: {
  requested: WritableSignal<TParams>;
  query: (params: TParams) => SearchQueryOptions<TData>;
  /** For a caller that keeps a copy of the requested parameters elsewhere. */
  onRevert?: (params: TParams) => void;
}) {
  const foutAfhandelingService = inject(FoutAfhandelingService);

  let searchedParams: TParams | undefined;

  const searchQuery = injectQuery(() => {
    // A copy, because some filter components write into the object they are
    // bound to. The parameters of the shown rows must stay as they were searched.
    const params = copyOf(requested());
    searchedParams = params;
    return {
      ...query(params),
      // Pairs the rows with their own parameters. After a change of key the
      // query result still holds the rows of the previous key for a moment, so
      // pairing them with `requested()` would mix up the two.
      select: (data: TData): ShownSearch<TParams, TData> => ({ data, params }),
      // A failed search is reported below, with what is shown instead.
      meta: { reportErrors: false },
    };
  });

  const shown = linkedSignal<
    ShownSearch<TParams, TData> | undefined,
    ShownSearch<TParams, TData> | undefined
  >({
    // Through a function: reading a field of the query result evaluates the
    // query options, which may read a required input that is not set yet.
    source: () => searchQuery.data(),
    computation: (result, previous) => result ?? previous?.value,
  });

  const reported = new WeakSet<object>();

  effect(() => {
    // Read on every result, so that the linked signal holds on to each success
    // even when nothing else reads it before the next search fails.
    shown();
    const error = searchQuery.error();
    if (!error || reported.has(error)) return;
    reported.add(error);
    untracked(() => fallBack(error));
  });

  function fallBack(error: HttpErrorResponse) {
    if (searchQuery.data() !== undefined) {
      report(error, { refetch: true });
      return;
    }

    const lastShown = shown();
    if (!lastShown) {
      report(error, { refetch: false });
      return;
    }

    const shownParams = copyOf(lastShown.params);
    requested.set(shownParams);
    onRevert?.(shownParams);
    report(error, {
      refetch: false,
      waarschuwing: "msg.zoeken.mislukt.vorige.zoekopdracht",
      titel: "dialoog.error.titel.zoeken.mislukt",
    });
  }

  function report(
    error: HttpErrorResponse,
    { refetch, ...options }: { refetch: boolean } & FoutAfhandelingOptions,
  ) {
    if (!(error instanceof HttpErrorResponse)) {
      console.error(error);
      return;
    }
    // As for every other read: a failed refetch still has its rows on screen,
    // so it only interrupts the user when the session has expired.
    if (refetch && !isLoggedOut(error)) {
      console.error(error);
      return;
    }
    foutAfhandelingService.foutAfhandelen(error, options);
  }

  return {
    query: searchQuery,
    requested: requested.asReadonly(),
    shown: shown.asReadonly(),
    data: computed(() => shown()?.data),
    params: computed(() => shown()?.params),
    /**
     * Searches with `params`. When those are the parameters already requested,
     * it searches again: to retry a search that failed, or to pick up changes
     * made on the server.
     */
    request(params: TParams) {
      // Compared with what was searched rather than with `requested()`, which
      // a filter component may have written into in place.
      const searchesAgain = hashKey([params]) === hashKey([searchedParams]);
      requested.set(params);
      if (searchesAgain) void searchQuery.refetch();
    },
    /** Forgets the shown search, until the next one succeeds. */
    clear() {
      shown.set(undefined);
    },
  };
}

/** The parameters are part of a query key, so they survive a trip through JSON. */
function copyOf<T>(params: T): T {
  return JSON.parse(JSON.stringify(params));
}

export type LastSuccessfulSearch<TParams, TData> = ReturnType<
  typeof injectLastSuccessfulSearch<TParams, TData>
>;
