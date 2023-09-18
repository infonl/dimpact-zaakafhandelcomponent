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
      params: FetchOptions<FilterKeys<Paths[P], "get">>["params"];
    },
  ) {
    return this.http.get<Response<P, "get">>(url, {
      params: (init?.params as any)?.path ?? undefined,
    });
  }

  public POST<P extends PathsWithMethod<Paths, "post">>(
    url: P,
    body: FetchOptions<FilterKeys<Paths[P], "post">>["body"],
    init?: Omit<Parameters<HttpClient["post"]>[2], "params"> & {
      params: FetchOptions<FilterKeys<Paths[P], "post">>["params"];
    },
  ) {
    return this.http.post<Response<P, "post">>(url, body, {
      ...this.getOptions(init),
    });
  }

  public PUT<P extends PathsWithMethod<Paths, "put">>(
    url: P,
    body: FetchOptions<FilterKeys<Paths[P], "put">>["body"],
    init?: Omit<Parameters<HttpClient["put"]>[2], "params"> & {
      params: FetchOptions<FilterKeys<Paths[P], "put">>["params"];
    },
  ) {
    return this.http.put<Response<P, "put">>(url, body, {
      ...this.getOptions(init),
    });
  }

  public DELETE<P extends PathsWithMethod<Paths, "delete">>(
    url: P,
    init?: Omit<Parameters<HttpClient["delete"]>[1], "params"> & {
      params: FetchOptions<FilterKeys<Paths[P], "delete">>["params"];
    },
  ) {
    return this.http.delete<Response<P, "delete">>(url, {
      ...this.getOptions(init),
    });
  }

  public PATCH<P extends PathsWithMethod<Paths, "patch">>(
    url: P,
    body: FetchOptions<FilterKeys<Paths[P], "patch">>["body"],
    init?: Omit<Parameters<HttpClient["patch"]>[2], "params"> & {
      params: FetchOptions<FilterKeys<Paths[P], "patch">>["params"];
    },
  ) {
    return this.http.patch<Response<P, "patch">>(url, body, {
      ...this.getOptions(init),
    });
  }

  private getOptions<T extends { params: any }>(init?: T) {
    const { params, ...rest } = init ?? {};
    return {
      params: (params as any)?.path ?? undefined,
      ...rest,
    };
  }
}
