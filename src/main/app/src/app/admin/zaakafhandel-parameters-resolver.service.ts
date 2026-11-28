/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Injectable } from "@angular/core";
import { ActivatedRouteSnapshot } from "@angular/router";
import { Observable } from "rxjs";
import { ZaakafhandelParameters } from "./model/zaakafhandel-parameters";
import { ZaakafhandelParametersService } from "./zaakafhandel-parameters.service";

@Injectable({
  providedIn: "root",
})
export class ZaakafhandelParametersResolver {
  constructor(private adminService: ZaakafhandelParametersService) {}

  resolve(route: ActivatedRouteSnapshot): Observable<ZaakafhandelParameters> {
    const uuid = route.paramMap.get("uuid");

    if (!uuid) {
      throw new Error(
        `${ZaakafhandelParametersResolver.name}: no 'uuid' parameter found in route`,
      );
    }

    return this.adminService.readZaakafhandelparameters(uuid);
  }
}
