/*
 * SPDX-FileCopyrightText: 2023 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { inject, Injectable } from "@angular/core";
import type { PathsWithMethod } from "openapi-typescript-helpers";
import { catchError } from "rxjs/operators";
import { FoutAfhandelingService } from "../../fout-afhandeling/fout-afhandeling.service";
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
import { HttpClient } from "./http-client";

@Injectable({
  providedIn: "root",
})
export class ZacHttpClient {
  private readonly foutAfhandelingService = inject(FoutAfhandelingService);
  private readonly httpClient = inject(HttpClient);

  public GET<
    Path extends PathsWithMethod<Paths, Method>,
    Method extends Methods = "get",
  >(url: Path, ...args: ArgsTuple<PathParameters<Path, Method>>) {
    return this.httpClient
      .GET<Path, Method>(url, ...args)
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
    return this.httpClient
      .POST<Path, Method>(url, body, ...args)
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
    return this.httpClient
      .PUT<Path, Method>(url, body, ...args)
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
    return this.httpClient
      .DELETE<Path, Method>(url, ...args)
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
    return this.httpClient
      .PATCH<Path, Method>(url, body, ...args)
      .pipe(
        catchError((error) =>
          this.foutAfhandelingService.foutAfhandelen(error),
        ),
      );
  }
}
