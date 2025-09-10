/*
 * SPDX-FileCopyrightText: 2023 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HttpClient, HttpHeaders } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { FetchOptions, FetchResponse } from "openapi-fetch";
import type {
  FilterKeys,
  HttpMethod,
  PathsWithMethod,
} from "openapi-typescript-helpers";
import { catchError } from "rxjs/operators";
import { paths } from "../../../generated/types/zac-openapi-types";
import { FoutAfhandelingService } from "../../fout-afhandeling/fout-afhandeling.service";
import { NullableIfOptional } from "../utils/generated-types";

type Paths = paths;
type Methods = Extract<HttpMethod, "get" | "post" | "put" | "delete" | "patch">;

type Response<Path extends keyof Paths, Method extends Methods> = NonNullable<
  NullableIfOptional<
    FetchResponse<
      Paths[Path][Method] extends Record<string | number, unknown>
        ? Paths[Path][Method]
        : never,
      Record<string, unknown>,
      "application/json"
    >["data"]
  >
>;

type PathParameters<
  Path extends PathsWithMethod<Paths, Method>,
  Method extends Methods,
> = FetchOptions<FilterKeys<Paths[Path], Method>>["params"];

type Body<
  Path extends PathsWithMethod<Paths, Method>,
  Method extends Methods,
> = NullableIfOptional<FetchOptions<FilterKeys<Paths[Path], Method>>["body"]>;

export type PostBody<
  Path extends PathsWithMethod<Paths, Method>,
  Method extends Methods = "post",
> = Body<Path, Method>;
export type PutBody<
  Path extends PathsWithMethod<Paths, Method>,
  Method extends Methods = "put",
> = Body<Path, Method>;
export type PatchBody<
  Path extends PathsWithMethod<Paths, Method>,
  Method extends Methods = "patch",
> = Body<Path, Method>;
export type DeleteBody<
  Path extends PathsWithMethod<Paths, Method>,
  Method extends Methods = "delete",
> = Body<Path, Method>;

type IsRequired<T> = keyof T extends never
  ? false
  : [T] extends [Record<string, never>]
    ? false
    : true;
type ArgsTuple<T> =
  IsRequired<T> extends true ? [parameters: T] : [parameters?: T];

@Injectable({
  providedIn: "root",
})
export class ZacHttpClient {
  constructor(
    private readonly http: HttpClient,
    private readonly foutAfhandelingService: FoutAfhandelingService,
  ) {}

  public GET<
    Path extends PathsWithMethod<Paths, Method>,
    Method extends Methods = "get",
  >(url: Path, ...args: ArgsTuple<PathParameters<Path, Method>>) {
    const parameters = args.at(0) ?? ({} as PathParameters<Path, Method>);
    return this.http
      .get<
        Response<Path, Method>
      >(this.formatUrl(url, parameters), this.addHttpOptions(parameters))
      .pipe(
        catchError((error) =>
          this.foutAfhandelingService.foutAfhandelen(error),
        ),
      );
  }

  public POST<
    Path extends PathsWithMethod<Paths, Method>,
    Method extends Methods = "post",
  >(
    url: Path,
    body: PostBody<Path, Method>,
    ...args: ArgsTuple<PathParameters<Path, Method>>
  ) {
    const parameters = args.at(0) ?? ({} as PathParameters<Path, Method>);

    return this.http
      .post<
        Response<Path, Method>
      >(this.formatUrl(url, parameters), body, this.addHttpOptions(parameters))
      .pipe(
        catchError((error) =>
          this.foutAfhandelingService.foutAfhandelen(error),
        ),
      );
  }

  public PUT<
    Path extends PathsWithMethod<Paths, Method>,
    Method extends Methods = "put",
  >(
    url: Path,
    body: PutBody<Path, Method>,
    ...args: ArgsTuple<PathParameters<Path, Method>>
  ) {
    const parameters = args.at(0) ?? ({} as PathParameters<Path, Method>);
    return this.http
      .put<
        Response<Path, Method>
      >(this.formatUrl(url, parameters), body, this.addHttpOptions(parameters))
      .pipe(
        catchError((error) =>
          this.foutAfhandelingService.foutAfhandelen(error),
        ),
      );
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
    const parameters =
      (args.at(0) as PathParameters<Path, Method>) ??
      ({} as PathParameters<Path, Method>);
    const body = args.at(1) ?? ({} as DeleteBody<Path, Method>);
    return this.http
      .delete<Response<Path, Method>>(this.formatUrl(url, parameters), {
        ...this.addHttpOptions(parameters),
        body: body,
      })
      .pipe(
        catchError((error) =>
          this.foutAfhandelingService.foutAfhandelen(error),
        ),
      );
  }

  public PATCH<
    Path extends PathsWithMethod<Paths, Method>,
    Method extends Methods = "patch",
  >(
    url: Path,
    body: PatchBody<Path, Method>,
    ...args: ArgsTuple<PathParameters<Path, Method>>
  ) {
    const parameters = args.at(0) ?? ({} as PathParameters<Path, Method>);
    return this.http
      .patch<
        Response<Path, "patch">
      >(this.formatUrl(url, parameters), body, this.addHttpOptions(parameters))
      .pipe(
        catchError((error) =>
          this.foutAfhandelingService.foutAfhandelen(error),
        ),
      );
  }

  private replacePathParams(
    urlTemplate: string,
    pathParams: Record<string, unknown>,
  ) {
    let url = urlTemplate;

    for (const [key, value] of Object.entries(pathParams)) {
      // Simple string replacement without regex
      const placeholder = `{${key}}`;
      while (url.includes(placeholder)) {
        if (value === null || value === undefined) {
          throw new HttpParamsError(
            `No key provided for '{${key}}', stopping request to '${urlTemplate}'`,
          );
        }

        url = url.replace(placeholder, `${value}`);
      }
    }

    return url;
  }

  private formatUrl<
    Path extends PathsWithMethod<Paths, Method>,
    Method extends Methods,
  >(url: string, parameters: PathParameters<Path, Method>) {
    if (
      parameters &&
      "path" in parameters &&
      typeof parameters.path === "object"
    ) {
      url = this.replacePathParams(url, parameters.path);
    }

    if (parameters?.query) {
      const queryParams = new URLSearchParams();
      for (const [key, value] of Object.entries(parameters.query)) {
        if (value !== undefined && value !== null) {
          queryParams.append(key, value.toString());
        }
      }
      const delimiter = url.includes("?") ? "&" : "?";
      url += `${delimiter}${queryParams.toString()}`;
    }

    return url;
  }

  private addHttpOptions<
    Path extends PathsWithMethod<Paths, Method>,
    Method extends Methods,
  >(
    parameters: PathParameters<Path, Method>,
  ): Parameters<typeof this.http.get>[1] {
    const result: Parameters<typeof this.http.get>[1] = {};

    if (parameters && "header" in parameters && parameters.header) {
      result.headers = new HttpHeaders(parameters.header);
    }

    if (parameters && "responseType" in parameters && parameters.responseType) {
      result.responseType = parameters.responseType as "json";
    }

    return result;
  }
}

export class HttpParamsError extends Error {}
