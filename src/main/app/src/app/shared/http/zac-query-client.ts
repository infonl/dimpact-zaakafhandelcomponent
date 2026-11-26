/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HttpErrorResponse } from "@angular/common/http";
import { inject, Injectable } from "@angular/core";
import {
  type MutationFunctionContext,
  mutationOptions,
  queryOptions,
} from "@tanstack/angular-query-experimental";
import type { PathsWithMethod } from "openapi-typescript-helpers";
import { filter, lastValueFrom, map, Observable, tap } from "rxjs";
import { UtilService } from "../../core/service/util.service";
import { FoutAfhandelingService } from "../../fout-afhandeling/fout-afhandeling.service";
import type {
  ArgsTuple,
  Body,
  DeleteBody,
  Methods,
  PatchBody,
  PathParameters,
  Paths,
  PostBody,
  PutBody,
  UploadProgress,
} from "./http-client";
import { HttpClient, Response } from "./http-client";
import {
  applyRule,
  type Rule,
  type WriteEndpoint,
  type WriteMethod,
  type WriteRequest,
} from "./invalidation";
import { INVALIDATION_MAP } from "./invalidation-map";
import { runLast } from "./merge-mutation-options";
import { reportsErrors } from "./query-client";

// From https://tanstack.com/query/latest/docs/framework/angular/guides/query-retries
const DEFAULT_RETRY_COUNT = 3;

const UPLOAD_PROGRESS_DESCRIPTION = "msg.document.uploaden.voortgang";

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

/** Refetches on every use and drops the data as soon as nothing observes it. */
export const LOAD_PER_VISIT = {
  staleTime: StaleTimes.Instant,
  gcTime: 0,
} as const;

/** The request of a write whose parameters are known before its body. */
const requestOf =
  <Parameters,>(args: [parameters?: Parameters]) =>
  <Body,>(body: Body) => ({ parameters: args.at(0), body });

@Injectable({
  providedIn: "root",
})
export class ZacQueryClient {
  private readonly foutAfhandelingService = inject(FoutAfhandelingService);
  private readonly httpClient = inject(HttpClient);
  private readonly utilService = inject(UtilService);

  /**
   * A mutation does not go through the query cache, so a failed write opens
   * the error dialog here. A mutation with `meta: { reportErrors: false }`
   * skips it, just as a query with that `meta` skips the cache's reporting.
   */
  private readonly reportError = (
    error: HttpErrorResponse,
    _variables: unknown,
    _context: unknown,
    { meta }: MutationFunctionContext,
  ) => {
    if (!reportsErrors(meta)) return;

    this.foutAfhandelingService.foutAfhandelen(error);
  };

  /**
   * The last `onSuccess` step, behind those of the service and the component:
   * a service may first cancel the reads that the save overtook, which would
   * also cancel a refresh.
   */
  private invalidation<
    Path extends PathsWithMethod<Paths, Method>,
    Method extends WriteMethod,
    Variables,
  >(
    method: Method,
    path: Path,
    toRequest: (variables: Variables) => Partial<WriteRequest<Path, Method>>,
  ) {
    // the compiler cannot follow the verb and the path through the key of the map
    const rule = INVALIDATION_MAP[
      `${method.toUpperCase()} ${path as string}` as WriteEndpoint
    ] as unknown as Rule<Path, Method>;
    // an endpoint without parameters or a body has none to give
    const toFullRequest = (variables: Variables) =>
      toRequest(variables) as WriteRequest<Path, Method>;

    if (rule.afterFailureToo) {
      return {
        onSettled: (
          _response: Response<Path, Method> | undefined,
          _error: unknown,
          variables: Variables,
          _onMutateResult: unknown,
          { client }: MutationFunctionContext,
        ) => applyRule(client, rule, toFullRequest(variables)),
      };
    }

    return {
      onSuccess: runLast(
        (
          response: Response<Path, Method>,
          variables: Variables,
          _onMutateResult: unknown,
          { client }: MutationFunctionContext,
        ) => applyRule(client, rule, { ...toFullRequest(variables), response }),
      ),
    };
  }

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

  /**
   * `POST` that shows how much of the request body has been sent on the global progress
   * indicator. Uploading a document of hundreds of megabytes otherwise leaves the user staring
   * at a spinner for minutes.
   */
  public POST_WITH_PROGRESS<Path extends PathsWithMethod<Paths, "post">>(
    url: Path,
    ...args: ArgsTuple<PathParameters<Path, "post">>
  ) {
    return this.mutationWithProgress<Path, "post">("post", url, args, (body) =>
      this.httpClient.POST_WITH_PROGRESS<Path, "post">(url, body, ...args),
    );
  }

  public PUT_WITH_PROGRESS<Path extends PathsWithMethod<Paths, "put">>(
    url: Path,
    ...args: ArgsTuple<PathParameters<Path, "put">>
  ) {
    return this.mutationWithProgress<Path, "put">("put", url, args, (body) =>
      this.httpClient.PUT_WITH_PROGRESS<Path, "put">(url, body, ...args),
    );
  }

  private mutationWithProgress<
    Path extends PathsWithMethod<Paths, Method>,
    Method extends WriteMethod,
  >(
    method: Method,
    url: Path,
    args: ArgsTuple<PathParameters<Path, Method>>,
    request: (
      body: Body<Path, Method>,
    ) => Observable<UploadProgress<Response<Path, Method>>>,
  ) {
    const reportProgress = (percentage: number) =>
      this.utilService.setProgress({
        percentage,
        description: UPLOAD_PROGRESS_DESCRIPTION,
      });

    return mutationOptions<
      Response<Path, Method>,
      HttpErrorResponse,
      Body<Path, Method>,
      void
    >({
      mutationKey: [url, ...args],
      mutationFn: (body: Body<Path, Method>) => {
        reportProgress(0);
        return lastValueFrom(
          request(body).pipe(
            tap((progress) => {
              if (progress.state === "uploading")
                reportProgress(progress.percentage);
            }),
            filter((progress) => progress.state === "done"),
            map(
              (progress) => (progress as { body: Response<Path, Method> }).body,
            ),
          ),
        );
      },
      ...this.invalidation<Path, Method, Body<Path, Method>>(
        method,
        url,
        requestOf(args),
      ),
      onSettled: () => this.utilService.setProgress(null),
      onError: this.reportError,
    });
  }

  public POST<Path extends PathsWithMethod<Paths, "post">>(
    url: Path,
    ...args: ArgsTuple<PathParameters<Path, "post">>
  ) {
    return mutationOptions<
      Response<Path, "post">,
      HttpErrorResponse,
      PostBody<Path, "post">,
      void
    >({
      mutationKey: [url, ...args],
      mutationFn: (body: PostBody<Path, "post">) =>
        lastValueFrom(this.httpClient.POST<Path, "post">(url, body, ...args)),
      ...this.invalidation<Path, "post", PostBody<Path, "post">>(
        "post",
        url,
        requestOf(args),
      ),
      onError: this.reportError,
    });
  }

  public PUT<Path extends PathsWithMethod<Paths, "put">>(
    url: Path,
    ...args: ArgsTuple<PathParameters<Path, "put">>
  ) {
    return mutationOptions<
      Response<Path, "put">,
      HttpErrorResponse,
      PutBody<Path, "put">,
      void
    >({
      mutationKey: [url, ...args],
      mutationFn: (body: PutBody<Path, "put">) =>
        lastValueFrom(this.httpClient.PUT<Path, "put">(url, body, ...args)),
      ...this.invalidation<Path, "put", PutBody<Path, "put">>(
        "put",
        url,
        requestOf(args),
      ),
      onError: this.reportError,
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
    Path extends PathsWithMethod<Paths, "delete">,
    // an endpoint without a request body has no variables of its own, so it is
    // mutated without an argument rather than with an explicit `undefined`
    Variables = [DeleteBody<Path, "delete">] extends [undefined]
      ? void
      : DeleteBody<Path, "delete">,
  >(
    url: Path,
    toRequest: (variables: Variables) => {
      parameters?: PathParameters<Path, "delete">;
      body?: DeleteBody<Path, "delete">;
    } = (variables) => ({ body: variables as DeleteBody<Path, "delete"> }),
  ) {
    return mutationOptions<
      Response<Path, "delete">,
      HttpErrorResponse,
      Variables,
      void
    >({
      mutationKey: [url],
      mutationFn: (variables: Variables) => {
        const { parameters, body } = toRequest(variables);

        return lastValueFrom(
          this.httpClient.DELETE<Path, "delete">(
            url,
            parameters as PathParameters<Path, "delete">,
            body,
          ),
        );
      },
      ...this.invalidation<Path, "delete", Variables>("delete", url, toRequest),
      onError: this.reportError,
    });
  }

  public PATCH<Path extends PathsWithMethod<Paths, "patch">>(
    url: Path,
    ...args: ArgsTuple<PathParameters<Path, "patch">>
  ) {
    return mutationOptions<
      Response<Path, "patch">,
      HttpErrorResponse,
      PatchBody<Path, "patch">,
      void
    >({
      mutationKey: [url, ...args],
      mutationFn: (body: PatchBody<Path, "patch">) =>
        lastValueFrom(this.httpClient.PATCH<Path, "patch">(url, body, ...args)),
      ...this.invalidation<Path, "patch", PatchBody<Path, "patch">>(
        "patch",
        url,
        requestOf(args),
      ),
      onError: this.reportError,
    });
  }
}
