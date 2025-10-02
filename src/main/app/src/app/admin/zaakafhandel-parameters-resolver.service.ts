/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Injectable } from "@angular/core";
import { ActivatedRouteSnapshot } from "@angular/router";
import { ZaakafhandelParametersService } from "./zaakafhandel-parameters.service";
import { forkJoin, map } from "rxjs";

@Injectable({
  providedIn: "root",
})
export class ZaakafhandelParametersResolver {
  constructor(private adminService: ZaakafhandelParametersService) {}

  resolve(route: ActivatedRouteSnapshot) {
    const uuid = route.paramMap.get("uuid");

    if (!uuid) {
      throw new Error(
        `${ZaakafhandelParametersResolver.name}: no 'uuid' parameter found in route`,
      );
    }

    return forkJoin({
      zaakafhandelParameters:
        this.adminService.readZaakafhandelparameters(uuid),
      zaakafhandelParametersBpmn:
        this.adminService.listBPMNZaakafhandelParameters(),
    }).pipe(
      map(({ zaakafhandelParameters, zaakafhandelParametersBpmn }) => {
        const isSavedZaakafhandelparameters = zaakafhandelParameters?.id;
        const isBpmn = zaakafhandelParametersBpmn.some(
          (item: any) =>
            item.zaaktypeUuid === zaakafhandelParameters.zaaktype.uuid,
        );
        return {
          zaakafhandelparameters: zaakafhandelParameters,
          isBpmn,
          isSavedZaakafhandelparameters,
        };
      }),
    );
  }
}
