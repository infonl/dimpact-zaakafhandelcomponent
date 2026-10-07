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
  PathParameters,
  Paths,
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

  /**
   * @deprecated Use ZacQueryClient.DELETE() for DELETE mutations.
   */
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
}
