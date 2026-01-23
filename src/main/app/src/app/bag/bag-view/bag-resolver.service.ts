/*
 * SPDX-FileCopyrightText: 2023 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Injectable } from "@angular/core";
import { ActivatedRouteSnapshot } from "@angular/router";
import { Observable } from "rxjs";
import { BAGService } from "../bag.service";
import { BAGObject } from "../model/bagobject";

@Injectable({
  providedIn: "root",
})
export class BAGResolverService {
  constructor(private bagService: BAGService) {}

  resolve(route: ActivatedRouteSnapshot): Observable<BAGObject> {
    const type = route.paramMap.get("type");
    const id = route.paramMap.get("id");

    if (!type || !id) {
      throw new Error(
        `${BAGResolverService.name}: 'type' or 'id' is not defined in route`,
      );
    }

    return this.bagService.read(type.toUpperCase(), id);
  }
}
