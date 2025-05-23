/*
 * SPDX-FileCopyrightText: 2023 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {HttpClient} from "@angular/common/http";
import { Injectable } from "@angular/core";
import createClient, { FetchOptions, FetchResponse } from "openapi-fetch";
import type { FilterKeys, HttpMethod } from "openapi-typescript-helpers";
import { catchError } from "rxjs/operators";
import { paths } from "../../../generated/types/zac-openapi-types";
import { FoutAfhandelingService } from "../../fout-afhandeling/fout-afhandeling.service";

createClient();
export type Paths = paths;

type PathsWithMethod<Paths, PathnameMethod extends HttpMethod> = {
  [Pathname in keyof Paths]: Paths[Pathname] extends {
    [K in PathnameMethod]: unknown;
  }
    ? Pathname
    : never;
}[keyof Paths];

type Response<
  P extends keyof Paths,
  type extends "get" | "post" | "put" | "delete" | "patch",
> = NonNullable<
  FetchResponse<
    Paths[P][type] extends Record<string | number, unknown>
      ? Paths[P][type]
      : never,
    Record<string, unknown>,
    "application/json"
  >["data"]
>;

@Injectable({
  providedIn: "root",
})
export class ZacHttpClient {
  constructor(
    private readonly http: HttpClient,
    private readonly foutAfhandelingService: FoutAfhandelingService,
  ) {}

  public GET<P extends PathsWithMethod<Paths, "get">>(
    url: P,
    init?: Parameters<HttpClient["get"]>[1] & {
      pathParams?: FetchOptions<FilterKeys<Paths[P], "get">>["params"];
    },
  ) {
    return this.http
      .get<Response<P, "get">>(this.prepareUrl(url, init?.pathParams), init)
      .pipe(
        catchError((error) =>
          this.foutAfhandelingService.foutAfhandelen(error),
        ),
      );
  }

  public POST<P extends PathsWithMethod<Paths, "post">>(
    url: P,
    body?: FetchOptions<FilterKeys<Paths[P], "post">>["body"],
    init?: Parameters<HttpClient["post"]>[2] & {
      pathParams?: FetchOptions<FilterKeys<Paths[P], "post">>["params"];
    },
  ) {
    return this.http
      .post<
        Response<P, "post">
      >(this.prepareUrl(url, init?.pathParams), body, init)
      .pipe(
        catchError((error) =>
          this.foutAfhandelingService.foutAfhandelen(error),
        ),
      );
  }

  public PUT<P extends PathsWithMethod<Paths, "put">>(
    url: P,
    body: FetchOptions<FilterKeys<Paths[P], "put">>["body"],
    init?: Parameters<HttpClient["put"]>[2] & {
      pathParams?: FetchOptions<FilterKeys<Paths[P], "put">>["params"];
    },
  ) {
    return this.http
      .put<
        Response<P, "put">
      >(this.prepareUrl(url, init?.pathParams), body, init)
      .pipe(
        catchError((error) =>
          this.foutAfhandelingService.foutAfhandelen(error),
        ),
      );
  }

  public DELETE<P extends PathsWithMethod<Paths, "delete">>(
    url: P,
    init?: Parameters<HttpClient["delete"]>[1] & {
      pathParams?: FetchOptions<FilterKeys<Paths[P], "delete">>["params"];
    },
  ) {
    return this.http
      .delete<
        Response<P, "delete">
      >(this.prepareUrl(url, init?.pathParams), init)
      .pipe(
        catchError((error) =>
          this.foutAfhandelingService.foutAfhandelen(error),
        ),
      );
  }

  public PATCH<P extends PathsWithMethod<Paths, "patch">>(
    url: P,
    body: FetchOptions<FilterKeys<Paths[P], "patch">>["body"],
    init?: Parameters<HttpClient["patch"]>[2] & {
      pathParams?: FetchOptions<FilterKeys<Paths[P], "patch">>["params"];
    },
  ) {
    return this.http
      .patch<
        Response<P, "patch">
      >(this.prepareUrl(url, init?.pathParams), body, init)
      .pipe(
        catchError((error) =>
          this.foutAfhandelingService.foutAfhandelen(error),
        ),
      );
  }

  private replacePathParams(
    urlTemplate: string,
    pathParams: Record<string, string | number | boolean | null>,
  ) {
    let url = urlTemplate;

    for (const key in pathParams) {
      // Simple string replacement without regex
      const placeholder = `{${key}}`;
      while (url.includes(placeholder)) {
        if (!pathParams[key]) {
          throw new HttpParamsError(
            `No key provided for '{${key}}', stopping request to '${urlTemplate}'`,
          );
        }

        url = url.replace(placeholder, pathParams[key].toString());
      }
    }

    return url;
  }

  private prepareUrl(
    url: string,
    pathParams?: {
      path?: Record<string, string | number | boolean | null>;
      query?: Record<string, string | number | boolean | null>;
    },
  ) {
    if (pathParams?.path) {
      url = this.replacePathParams(url, pathParams.path);
    }

    if (pathParams?.query) {
      const queryParams = new URLSearchParams();
      for (const [key, value] of Object.entries(pathParams.query)) {
        if (value !== undefined && value !== null) {
          queryParams.append(key, value.toString());
        }
      }
      const delimiter = url.includes("?") ? "&" : "?";
      url += `${delimiter}${queryParams.toString()}`;
    }

    return url;
  }
}

export class HttpParamsError extends Error {}
