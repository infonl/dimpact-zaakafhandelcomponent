/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Injectable } from "@angular/core";
import { ActivatedRouteSnapshot } from "@angular/router";
import { ReferentieTabelService } from "./referentie-tabel.service";

@Injectable({
  providedIn: "root",
})
export class ReferentieTabelResolver {
  constructor(private adminService: ReferentieTabelService) {}

  resolve(route: ActivatedRouteSnapshot) {
    const id = route.paramMap.get("id");
    if (!id) {
      throw new Error(
        `${ReferentieTabelResolver.name}: 'id' in not defined in route`,
      );
    }

    const idAsNumber = Number(id);
    if (isNaN(idAsNumber)) return null;

    return this.adminService.readReferentieTabel(idAsNumber);
  }
}
