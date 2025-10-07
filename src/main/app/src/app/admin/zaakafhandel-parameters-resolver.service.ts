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
      listBpmnZaakafhandelParameters:
        this.adminService.listBpmnZaakafhandelParameters(),
    }).pipe(
      map(({ zaakafhandelParameters, listBpmnZaakafhandelParameters }) => {
        const bpmnZaakafhandelParameters = listBpmnZaakafhandelParameters?.find(
          (item) => item.zaaktypeUuid === zaakafhandelParameters.zaaktype.uuid,
        );

        return {
          zaakafhandelparameters: zaakafhandelParameters, // full list of zaakafhandelparameters, both CMMN and BPMN
          listBpmnZaakafhandelParameters, // full list of BPMN zaak afhandelparameters
          bpmnZaakafhandelParameters: {
            ...bpmnZaakafhandelParameters,
            zaaktype: zaakafhandelParameters.zaaktype, // will be put in endpoint in backend PR!
          },
          isBpmn: !!bpmnZaakafhandelParameters, // true if there is a matching BPMN process definition
          isSavedZaakafhandelparameters: !!zaakafhandelParameters?.id, // true if zaakafhandelparameters for this zaaktype has been saved before
        };
      }),
    );
  }
}
