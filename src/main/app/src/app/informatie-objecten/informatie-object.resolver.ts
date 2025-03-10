/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Injectable } from "@angular/core";
import { ActivatedRouteSnapshot } from "@angular/router";
import { InformatieObjectenService } from "./informatie-objecten.service";

@Injectable({
  providedIn: "root",
})
export class InformatieObjectResolver {
  constructor(private informatieObjectenService: InformatieObjectenService) {}

  resolve(route: ActivatedRouteSnapshot) {
    const informatieObjectUUID = route.paramMap.get("uuid");
    const informatieObjectVersie = route.paramMap.get("versie");

    if (!informatieObjectUUID) {
      throw new Error(
        `${InformatieObjectResolver.name}: 'uuid' is not defined in route`,
      );
    }

    if (informatieObjectVersie) {
      return this.informatieObjectenService.readEnkelvoudigInformatieobjectVersie(
        informatieObjectUUID,
        Number(informatieObjectVersie),
      );
    }
    return this.informatieObjectenService.readEnkelvoudigInformatieobject(
      informatieObjectUUID,
    );
  }
}
