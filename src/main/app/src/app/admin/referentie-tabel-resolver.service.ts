/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Injectable } from "@angular/core";
import { ActivatedRouteSnapshot } from "@angular/router";
import { Observable } from "rxjs";
import { ReferentieTabel } from "./model/referentie-tabel";
import { ReferentieTabelService } from "./referentie-tabel.service";

@Injectable({
  providedIn: "root",
})
export class ReferentieTabelResolver {
  constructor(private adminService: ReferentieTabelService) {}

  resolve(route: ActivatedRouteSnapshot): Observable<ReferentieTabel> {
    const id = route.paramMap.get("id");
    if (!id) {
      throw new Error(
        `${ReferentieTabelResolver.name}: 'id' in not defined in route`,
      );
    }

    return this.adminService.readReferentieTabel(id);
  }
}
