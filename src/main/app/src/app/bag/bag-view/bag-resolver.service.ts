/*
 * SPDX-FileCopyrightText: 2023 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Injectable } from "@angular/core";
import { ActivatedRouteSnapshot } from "@angular/router";
import { GeneratedType } from "../../shared/utils/generated-types";
import { BAGService } from "../bag.service";

@Injectable({
  providedIn: "root",
})
export class BAGResolverService {
  constructor(private readonly bagService: BAGService) {}

  resolve(route: ActivatedRouteSnapshot) {
    const type = route.paramMap.get("type");
    const id = route.paramMap.get("id");

    if (!type || !id) {
      throw new Error(
        `${BAGResolverService.name}: 'type' or 'id' is not defined in route`,
      );
    }

    return this.bagService.read(
      type.toUpperCase() as GeneratedType<"BAGObjectType">,
      id,
    );
  }
}
