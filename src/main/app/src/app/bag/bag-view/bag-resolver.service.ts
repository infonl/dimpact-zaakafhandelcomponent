/*
 * SPDX-FileCopyrightText: 2023 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { inject, Injectable } from "@angular/core";
import { ActivatedRouteSnapshot } from "@angular/router";
import { QueryClient } from "@tanstack/angular-query-experimental";
import { StaleTimes } from "../../shared/http/zac-query-client";
import { GeneratedType } from "../../shared/utils/generated-types";
import { BAGService } from "../bag.service";

@Injectable({
  providedIn: "root",
})
export class BAGResolverService {
  private readonly bagService = inject(BAGService);
  private readonly queryClient = inject(QueryClient);

  resolve(route: ActivatedRouteSnapshot) {
    const type = route.paramMap.get("type");
    const id = route.paramMap.get("id");

    if (!type || !id) {
      throw new Error(
        `${BAGResolverService.name}: 'type' or 'id' is not defined in route`,
      );
    }

    return this.queryClient.fetchQuery({
      ...this.bagService.read(
        type.toUpperCase() as GeneratedType<"BagObjectType">,
        id,
      ),
      staleTime: StaleTimes.Instant,
    });
  }
}
