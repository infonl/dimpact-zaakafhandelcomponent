/*
 * SPDX-FileCopyrightText: 2023 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { inject, Injectable } from "@angular/core";
import { queryOptions } from "@tanstack/angular-query-experimental";
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

@Injectable({
  providedIn: "root",
})
export class ZacQueryClient {
  private readonly httpClient = inject(HttpClient);

  public GET<
    Path extends PathsWithMethod<Paths, Method>,
    Method extends Methods = "get",
  >(url: Path, ...args: ArgsTuple<PathParameters<Path, Method>>) {
    return queryOptions<Response<Path, Method>>({
      queryKey: [url, ...args],
      queryFn: () =>
        lastValueFrom(this.httpClient.GET<Path, Method>(url, ...args)),
    });
  }

  public POST<
    Path extends PathsWithMethod<Paths, Method>,
    Method extends Methods = "post",
  >(
    url: Path,
    body: PostBody<Path, Method>,
    ...args: ArgsTuple<PathParameters<Path, Method>>
  ) {
    return queryOptions<Response<Path, Method>>({
      queryKey: [url, ...args],
      queryFn: () =>
        lastValueFrom(this.httpClient.POST<Path, Method>(url, body, ...args)),
    });
  }

  public PUT<
    Path extends PathsWithMethod<Paths, Method>,
    Method extends Methods = "put",
  >(
    url: Path,
    body: PutBody<Path, Method>,
    ...args: ArgsTuple<PathParameters<Path, Method>>
  ) {
    return queryOptions<Response<Path, Method>>({
      queryKey: [url, ...args],
      queryFn: () =>
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
    return queryOptions<Response<Path, Method>>({
      queryKey: [url, ...args],
      queryFn: () =>
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
    return queryOptions<Response<Path, Method>>({
      queryKey: [url, ...args],
      queryFn: () =>
        lastValueFrom(this.httpClient.PATCH<Path, Method>(url, body, ...args)),
    });
  }
}
