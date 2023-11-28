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
type Paths = paths;

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
    init?: Omit<Parameters<HttpClient["get"]>[1], "params"> & {
      pathParams: FetchOptions<FilterKeys<Paths[P], "get">>["params"];
    },
  ) {
    const { pathParams, ...restInit } = { pathParams: {}, ...init };
    return this.http.get<Response<P, "get">>(
      this.prepareUrl(url, init),
      restInit,
    );
  }

  public POST<P extends PathsWithMethod<Paths, "post">>(
    url: P,
    body: FetchOptions<FilterKeys<Paths[P], "post">>["body"],
    init?: Omit<Parameters<HttpClient["post"]>[2], "params"> & {
      pathParams: FetchOptions<FilterKeys<Paths[P], "post">>["params"];
    },
  ) {
    const { pathParams, ...restInit } = { pathParams: {}, ...init };
    return this.http.post<Response<P, "post">>(
      this.prepareUrl(url, init),
      body,
      init,
    );
  }

  public PUT<P extends PathsWithMethod<Paths, "put">>(
    url: P,
    body: FetchOptions<FilterKeys<Paths[P], "put">>["body"],
    init?: Omit<Parameters<HttpClient["put"]>[2], "params"> & {
      pathParams: FetchOptions<FilterKeys<Paths[P], "put">>["params"];
    },
  ) {
    const { pathParams, ...restInit } = { pathParams: {}, ...init };
    return this.http.put<Response<P, "put">>(
      this.prepareUrl(url, init),
      body,
      restInit,
    );
  }

  public DELETE<P extends PathsWithMethod<Paths, "delete">>(
    url: P,
    init?: Omit<Parameters<HttpClient["delete"]>[1], "params"> & {
      pathParams: FetchOptions<FilterKeys<Paths[P], "delete">>["params"];
    },
  ) {
    const { pathParams, ...restInit } = { pathParams: {}, ...init };
    return this.http.delete<Response<P, "delete">>(
      this.prepareUrl(url, restInit),
      restInit,
    );
  }

  public PATCH<P extends PathsWithMethod<Paths, "patch">>(
    url: P,
    body: FetchOptions<FilterKeys<Paths[P], "patch">>["body"],
    init?: Omit<Parameters<HttpClient["patch"]>[2], "params"> & {
      pathParams: FetchOptions<FilterKeys<Paths[P], "patch">>["params"];
    },
  ) {
    const { pathParams, ...restInit } = { pathParams: {}, ...init };
    return this.http.patch<Response<P, "patch">>(
      this.prepareUrl(url, init),
      body,
      restInit,
    );
  }

  private replacePathParams(
    urlTemplate: string,
    pathParams: Record<string, string | number | boolean>,
  ): string {
    let url = urlTemplate;
    Object.keys(pathParams).forEach((key) => {
      url = url.replace(
        new RegExp(`{${key}}`, "g"),
        pathParams[key].toString(),
      );
    });
    return url;
  }

  private prepareUrl(url: string, init?: any) {
    const pathParams = init?.pathParams;
    let newUrl = url as string;
    if (pathParams && "path" in pathParams) {
      newUrl = this.replacePathParams(url, pathParams.path);
    }
    return newUrl;
  }
}
