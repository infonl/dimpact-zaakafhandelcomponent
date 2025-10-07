/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Injectable } from "@angular/core";
import { ActivatedRouteSnapshot } from "@angular/router";
import { forkJoin, map } from "rxjs";
import { ProcessDefinitionsService } from "./process-definitions.service";
import { ZaakafhandelParametersService } from "./zaakafhandel-parameters.service";

@Injectable({
  providedIn: "root",
})
export class ZaakafhandelParametersResolver {
  constructor(
    private readonly adminService: ZaakafhandelParametersService,
    private readonly processDefinitionsService: ProcessDefinitionsService,
  ) {}

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
      bpmnZaakafhandelParametersList:
        this.adminService.listBpmnZaakafhandelParameters(),
      bpmnProcessDefinitionsList:
        this.processDefinitionsService.listProcessDefinitions(),
    }).pipe(
      map(
        ({
          zaakafhandelParameters,
          bpmnZaakafhandelParametersList,
          bpmnProcessDefinitionsList,
        }) => {
          const bpmnZaakafhandelParameters =
            bpmnZaakafhandelParametersList?.find(
              (item) =>
                item.zaaktypeUuid === zaakafhandelParameters.zaaktype.uuid,
            );

          return {
            zaakafhandelParameters, // zaakafhandelparameters of this zaak
            bpmnProcessDefinitionsList, // full list of BPMN process definitions
            bpmnZaakafhandelParametersList, // full list of BPMN zaak afhandelparameters
            bpmnZaakafhandelParameters: {
              ...bpmnZaakafhandelParameters,
              zaaktype: zaakafhandelParameters.zaaktype, // will be put in endpoint in backend PR!
            },
            isBpmn: !!bpmnZaakafhandelParameters, // true if there is a matching BPMN process definition
            isSavedZaakafhandelparameters:
              !!zaakafhandelParameters?.id || !!bpmnZaakafhandelParameters?.id, // true if zaakafhandelparameters or BPMN zaakafhandelparameters for this zaaktype has been saved before (id is set on save)
          };
        },
      ),
    );
  }
}
