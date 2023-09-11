import createClient, { FetchOptions, FetchResponse } from "openapi-fetch";

import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core";
import type { FilterKeys } from "openapi-typescript-helpers";
import { paths } from "schema";

type Paths = paths;

@Injectable({
  providedIn: "root",
})
export class ZacHttpClient {
  constructor(private http: HttpClient) {}

  public GET<P extends keyof Paths>(
    url: P,
    init?: Omit<Parameters<HttpClient["get"]>[1], "params" | "headers"> & {
      params: FetchOptions<FilterKeys<Paths[P], "get">>;
    },
  ) {
    return this.http.get<
      FetchResponse<
        "get" extends infer T
          ? T extends "get"
            ? T extends keyof Paths[P]
              ? Paths[P][T]
              : unknown
            : never
          : never
      >["data"]
    >(url, { params: (init?.params as any)?.path ?? undefined });
  }
}
