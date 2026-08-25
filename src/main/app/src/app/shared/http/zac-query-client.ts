/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HttpErrorResponse } from "@angular/common/http";
import { inject, Injectable } from "@angular/core";
import {
  mutationOptions,
  queryOptions,
} from "@tanstack/angular-query-experimental";
import type { PathsWithMethod } from "openapi-typescript-helpers";
import { lastValueFrom } from "rxjs";
import { FoutAfhandelingService } from "../../fout-afhandeling/fout-afhandeling.service";
import type {
  ArgsTuple,
  DeleteBody,
  Methods,
  PatchBody,
  PathParameters,
  Paths,
  PostBody,
  PutBody,
} from "./http-client";
import { HttpClient, Response } from "./http-client";

// From https://tanstack.com/query/latest/docs/framework/angular/guides/query-retries
const DEFAULT_RETRY_COUNT = 3;

/** Retries only what could still succeed: a dropped connection or a server fault. */
const retryOnServerError = (failureCount: number, error: HttpErrorResponse) => {
  if (failureCount >= DEFAULT_RETRY_COUNT) return false;
  return error.status === 0 || error.status >= 500;
};

export enum StaleTimes {
  Infinite = Infinity,
  Long = 5 * 60 * 1000,
  Medium = 60 * 1000,
  Short = 15 * 1000,
  Instant = 0,
}

@Injectable({
  providedIn: "root",
})
export class ZacQueryClient {
  private readonly foutAfhandelingService = inject(FoutAfhandelingService);
  private readonly httpClient = inject(HttpClient);

  public GET<
    Path extends PathsWithMethod<Paths, Method>,
    Method extends Methods = "get",
  >(url: Path, ...args: ArgsTuple<PathParameters<Path, Method>>) {
    return queryOptions<Response<Path, Method>, HttpErrorResponse>({
      queryKey: [url, ...args],
      queryFn: () =>
        lastValueFrom(this.httpClient.GET<Path, Method>(url, ...args)),
      retry: retryOnServerError,
      refetchOnWindowFocus: false,
      staleTime: StaleTimes.Long,
      gcTime: StaleTimes.Long * 2,
    });
  }

  public POST<
    Path extends PathsWithMethod<Paths, Method>,
    Method extends Methods = "post",
  >(url: Path, ...args: ArgsTuple<PathParameters<Path, Method>>) {
    return mutationOptions<
      Response<Path, Method>,
      HttpErrorResponse,
      PostBody<Path, Method>,
      void
    >({
      mutationKey: [url, ...args],
      mutationFn: (body: PostBody<Path, Method>) =>
        lastValueFrom(this.httpClient.POST<Path, Method>(url, body, ...args)),
      onError: (error) => this.foutAfhandelingService.foutAfhandelen(error),
    });
  }

  public PUT<
    Path extends PathsWithMethod<Paths, Method>,
    Method extends Methods = "put",
  >(url: Path, ...args: ArgsTuple<PathParameters<Path, Method>>) {
    return mutationOptions<
      Response<Path, Method>,
      HttpErrorResponse,
      PutBody<Path, Method>,
      void
    >({
      mutationKey: [url, ...args],
      mutationFn: (body: PutBody<Path, Method>) =>
        lastValueFrom(this.httpClient.PUT<Path, Method>(url, body, ...args)),
      onError: (error) => this.foutAfhandelingService.foutAfhandelen(error),
    });
  }

  /**
   * A search endpoint takes its filters in a request body, so it reads over `PUT`.
   * The body is part of the query key, which is what makes one set of filters
   * cacheable and invalidatable apart from the next.
   */
  public PUT_QUERY<
    Path extends PathsWithMethod<Paths, Method>,
    Method extends Methods = "put",
  >(
    url: Path,
    body: PutBody<Path, Method>,
    ...args: ArgsTuple<PathParameters<Path, Method>>
  ) {
    return queryOptions<Response<Path, Method>, HttpErrorResponse>({
      queryKey: [url, body, ...args],
      queryFn: () =>
        lastValueFrom(this.httpClient.PUT<Path, Method>(url, body, ...args)),
      retry: retryOnServerError,
      refetchOnWindowFocus: false,
      staleTime: StaleTimes.Short,
      gcTime: StaleTimes.Short * 2,
    });
  }

  /**
   * The variables are whatever the caller mutates with — the row of a table, an
   * id, a form value — and `toRequest` derives the path parameters and the body
   * from them. That keeps one set of options usable for every item the user can
   * click, rather than one per item, and gives every callback the item it acted
   * on. Endpoints without path parameters can leave it out: the variables are
   * then the request body.
   */
  public DELETE<
    Path extends PathsWithMethod<Paths, Method>,
    Method extends Methods = "delete",
    // an endpoint without a request body has no variables of its own, so it is
    // mutated without an argument rather than with an explicit `undefined`
    Variables = [DeleteBody<Path, Method>] extends [undefined]
      ? void
      : DeleteBody<Path, Method>,
  >(
    url: Path,
    toRequest: (variables: Variables) => {
      parameters?: PathParameters<Path, Method>;
      body?: DeleteBody<Path, Method>;
    } = (variables) => ({ body: variables as DeleteBody<Path, Method> }),
  ) {
    return mutationOptions<
      Response<Path, Method>,
      HttpErrorResponse,
      Variables,
      void
    >({
      mutationKey: [url],
      mutationFn: (variables: Variables) => {
        const { parameters, body } = toRequest(variables);

        return lastValueFrom(
          this.httpClient.DELETE<Path, Method>(
            url,
            parameters as PathParameters<Path, Method>,
            body,
          ),
        );
      },
      onError: (error) => this.foutAfhandelingService.foutAfhandelen(error),
    });
  }

  public PATCH<
    Path extends PathsWithMethod<Paths, Method>,
    Method extends Methods = "patch",
  >(url: Path, ...args: ArgsTuple<PathParameters<Path, Method>>) {
    // eslint-disable-next-line @typescript-eslint/ban-ts-comment -- @ts-expect-error causes TS2578 in Angular esbuild build (Ivy does not reproduce TS2590)
    // @ts-ignore TS2590: Expression produces a union type that is too complex to represent (tsc only; esbuild/Ivy does not reproduce)
    return mutationOptions<
      Response<Path, Method>,
      HttpErrorResponse,
      PatchBody<Path, Method>,
      void
    >({
      mutationKey: [url, ...args],
      mutationFn: (body: PatchBody<Path, Method>) =>
        lastValueFrom(this.httpClient.PATCH<Path, Method>(url, body, ...args)),
      onError: (error) => this.foutAfhandelingService.foutAfhandelen(error),
    });
  }
}
