/*
 * SPDX-FileCopyrightText: 2023 INFO.nl
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
import type {
  ArgsTuple,
  DeleteBody,
  IsRequired,
  Methods,
  PatchBody,
  PathParameters,
  Paths,
  PostBody,
  PutBody,
} from "./http-client";
import { HttpClient, Response } from "./http-client";

// From https://tanstack.com/query/latest/docs/framework/angular/guides/query-retries
export const DEFAULT_RETRY_COUNT = 3;

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
  private readonly httpClient = inject(HttpClient);

  public GET<
    Path extends PathsWithMethod<Paths, Method>,
    Method extends Methods = "get",
  >(url: Path, ...args: ArgsTuple<PathParameters<Path, Method>>) {
    return queryOptions<Response<Path, Method>, HttpErrorResponse>({
      queryKey: [url, ...args],
      queryFn: () =>
        lastValueFrom(this.httpClient.GET<Path, Method>(url, ...args)),
      retry: (failureCount: number, error) => {
        if (failureCount >= DEFAULT_RETRY_COUNT) {
          return false;
        }
        return error.status === 0 || error.status >= 500;
      },
      refetchOnWindowFocus: true,
      staleTime: StaleTimes.Instant,
      gcTime: StaleTimes.Long * 2,
    });
  }

  public POST<
    Path extends PathsWithMethod<Paths, Method>,
    Method extends Methods = "post",
  >(url: Path, ...args: ArgsTuple<PathParameters<Path, Method>>) {
    return mutationOptions({
      mutationKey: [url, ...args],
      mutationFn: (body: PostBody<Path, Method>) =>
        lastValueFrom(this.httpClient.POST<Path, Method>(url, body, ...args)),
    });
  }

  public PUT<
    Path extends PathsWithMethod<Paths, Method>,
    Method extends Methods = "put",
  >(url: Path, ...args: ArgsTuple<PathParameters<Path, Method>>) {
    return mutationOptions({
      mutationKey: [url, ...args],
      mutationFn: (body: PutBody<Path, Method>) =>
        lastValueFrom(this.httpClient.PUT<Path, Method>(url, body, ...args)),
    });
  }

  public DELETE<
    Path extends PathsWithMethod<Paths, Method>,
    Method extends Methods = "delete",
  >(
    url: Path,
    ...args: IsRequired<PathParameters<Path, Method>> extends true
      ? [
          parameters: PathParameters<Path, Method>,
          body?: DeleteBody<Path, Method>,
        ]
      : [
          parameters?: PathParameters<Path, Method>,
          body?: DeleteBody<Path, Method>,
        ]
  ) {
    return mutationOptions<Response<Path, Method>>({
      mutationKey: [url, ...args],
      mutationFn: () =>
        lastValueFrom(this.httpClient.DELETE<Path, Method>(url, ...args)),
    });
  }

  public PATCH<
    Path extends PathsWithMethod<Paths, Method>,
    Method extends Methods = "patch",
  >(
    url: Path,
    body: PatchBody<Path, Method>,
    ...args: ArgsTuple<PathParameters<Path, Method>>
  ) {
    // @ts-expect-error Expression produces a union type that is too complex to represent.
    return mutationOptions<Response<Path, Method>>({
      mutationKey: [url, ...args],
      mutationFn: () =>
        lastValueFrom(this.httpClient.PATCH<Path, Method>(url, body, ...args)),
    });
  }
}
