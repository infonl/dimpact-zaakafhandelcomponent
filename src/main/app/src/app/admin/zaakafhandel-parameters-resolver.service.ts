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
      bpmnProcessDefinitions:
        this.adminService.listBpmnZaakafhandelParameters(),
    }).pipe(
      map(({ zaakafhandelParameters, bpmnProcessDefinitions }) => {
        const isSavedZaakafhandelparameters = zaakafhandelParameters?.id;
        const bpmnProcessDefinition = bpmnProcessDefinitions?.find(
          (item: any) =>
            item.zaaktypeUuid === zaakafhandelParameters.zaaktype.uuid,
        );
        return {
          zaakafhandelparameters: zaakafhandelParameters, // full list of zaakafhandelparameters, both CMMN and BPMN
          bpmnProcessDefinitions, // full list of BPMN process definitions
          bpmnProcessDefinition, // the BPMN process definition that matches the zaakafhandelparameters (if any)
          isBpmn: !!bpmnProcessDefinition, // true if there is a matching BPMN process definition
          isSavedZaakafhandelparameters, // true if zaakafhandelparameters for this zaaktype has been saved before
        };
      }),
    );
  }
}
