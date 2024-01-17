/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core";
import createClient, { FetchOptions, FetchResponse } from "openapi-fetch";
import type { FilterKeys, HttpMethod } from "openapi-typescript-helpers";
import { paths } from "src/generated/types/zac-openapi-types";

createClient();
export type Paths = paths;

type PathsWithMethod<Paths, PathnameMethod extends HttpMethod> = {
  [Pathname in keyof Paths]: Paths[Pathname] extends {
    [K in PathnameMethod]: any;
  }
    ? Pathname
    : never;
}[keyof Paths];

type Response<
  P extends keyof Paths,
  type extends "get" | "post" | "put" | "delete" | "patch",
> = FetchResponse<
  type extends infer T
    ? T extends type
      ? T extends keyof Paths[P]
        ? Paths[P][T]
        : unknown
      : never
    : never
>["data"];

@Injectable({
  providedIn: "root",
})
export class ZacHttpClient {
  constructor(private http: HttpClient) {}

  public GET<P extends PathsWithMethod<Paths, "get">>(
    url: P,
    init?: Parameters<HttpClient["get"]>[1] & {
      pathParams: FetchOptions<FilterKeys<Paths[P], "get">>["params"];
    },
  ) {
    return this.http.get<Response<P, "get">>(
      this.prepareUrl(url, init?.pathParams),
      init,
    );
  }

  public POST<P extends PathsWithMethod<Paths, "post">>(
    url: P,
    body?: FetchOptions<FilterKeys<Paths[P], "post">>["body"],
    init?: Parameters<HttpClient["post"]>[2] & {
      pathParams: FetchOptions<FilterKeys<Paths[P], "post">>["params"];
    },
  ) {
    return this.http.post<Response<P, "post">>(
      this.prepareUrl(url, init?.pathParams),
      body,
      init,
    );
  }

  public PUT<P extends PathsWithMethod<Paths, "put">>(
    url: P,
    body: FetchOptions<FilterKeys<Paths[P], "put">>["body"],
    init?: Parameters<HttpClient["put"]>[2] & {
      pathParams: FetchOptions<FilterKeys<Paths[P], "put">>["params"];
    },
  ) {
    return this.http.put<Response<P, "put">>(
      this.prepareUrl(url, init?.pathParams),
      body,
      init,
    );
  }

  public DELETE<P extends PathsWithMethod<Paths, "delete">>(
    url: P,
    init?: Parameters<HttpClient["delete"]>[1] & {
      pathParams: FetchOptions<FilterKeys<Paths[P], "delete">>["params"];
    },
  ) {
    return this.http.delete<Response<P, "delete">>(
      this.prepareUrl(url, init?.pathParams),
      init,
    );
  }

  public PATCH<P extends PathsWithMethod<Paths, "patch">>(
    url: P,
    body: FetchOptions<FilterKeys<Paths[P], "patch">>["body"],
    init?: Parameters<HttpClient["patch"]>[2] & {
      pathParams: FetchOptions<FilterKeys<Paths[P], "patch">>["params"];
    },
  ) {
    return this.http.patch<Response<P, "patch">>(
      this.prepareUrl(url, init?.pathParams),
      body,
      init,
    );
  }

  private replacePathParams(
    urlTemplate: string,
    pathParams: Record<string, string | number | boolean>,
  ): string {
    let url = urlTemplate;
    for (const key in pathParams) {
      if (pathParams.hasOwnProperty(key)) {
        // Simple string replacement without regex
        const placeholder = `{${key}}`;
        while (url.includes(placeholder)) {
          url = url.replace(placeholder, pathParams[key].toString());
        }
      }
    }
    return url;
  }

  private prepareUrl(url: string, pathParams?: any) {
    let newUrl = url;
    if (pathParams) {
      newUrl = this.replacePathParams(url, pathParams.path);
    }
    return newUrl;
  }
}
