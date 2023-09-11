import createClient, { FetchOptions, FetchResponse } from "openapi-fetch";

import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core";
import type { FilterKeys } from "openapi-typescript-helpers";
import { paths } from "src/generated/types/zac-openapi-types";

type Paths = paths;

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

  public GET<P extends keyof Paths>(
    url: P,
    init?: Omit<Parameters<HttpClient["get"]>[1], "params"> & {
      params: FetchOptions<FilterKeys<Paths[P], "get">>;
    },
  ) {
    return this.http.get<Response<P, "get">>(url, {
      params: (init?.params as any)?.path ?? undefined,
    });
  }

  public POST<P extends keyof Paths>(
    url: P,
    body: FetchOptions<FilterKeys<Paths[P], "post">>["body"],
    init?: Parameters<HttpClient["post"]>[2] & {
      params: FetchOptions<FilterKeys<Paths[P], "post">>;
    },
  ) {
    return this.http.post<Response<P, "post">>(url, body, {
      ...this.getOptions(init),
    });
  }

  public PUT<P extends keyof Paths>(
    url: P,
    body: FetchOptions<FilterKeys<Paths[P], "put">>["body"],
    init?: Parameters<HttpClient["put"]>[2] & {
      params: FetchOptions<FilterKeys<Paths[P], "put">>;
    },
  ) {
    return this.http.put<Response<P, "put">>(url, body, {
      ...this.getOptions(init),
    });
  }

  public DELETE<P extends keyof Paths>(
    url: P,
    init?: Parameters<HttpClient["delete"]>[1] & {
      params: FetchOptions<FilterKeys<Paths[P], "delete">>;
    },
  ) {
    return this.http.delete<Response<P, "delete">>(url, {
      ...this.getOptions(init),
    });
  }

  public PATCH<P extends keyof Paths>(
    url: P,
    body: FetchOptions<FilterKeys<Paths[P], "patch">>["body"],
    init?: Parameters<HttpClient["patch"]>[2] & {
      params: FetchOptions<FilterKeys<Paths[P], "patch">>;
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
