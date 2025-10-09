/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Injectable } from "@angular/core";
import { ActivatedRouteSnapshot } from "@angular/router";
import { forkJoin, map } from "rxjs";
import { ProcessDefinitionsService } from "./process-definitions.service";
import { ZaakafhandelParametersService } from "./zaakafhandel-parameters.service";
import { ConfiguratieService } from "../configuratie/configuratie.service";

@Injectable({
  providedIn: "root",
})
export class ZaakafhandelParametersResolver {
  constructor(
    private readonly zaakafhandelParametersService: ZaakafhandelParametersService,
    private readonly processDefinitionsService: ProcessDefinitionsService,
    private readonly configuratieService: ConfiguratieService,
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
        this.zaakafhandelParametersService.readZaakafhandelparameters(uuid),
      bpmnZaakafhandelParametersList:
        this.zaakafhandelParametersService.listBpmnZaakafhandelParameters(),
      bpmnProcessDefinitionsList:
        this.processDefinitionsService.listProcessDefinitions(),
      bmpnFeatureFlag: this.configuratieService.readFeatureFlagBpmnSupport(),
    }).pipe(
      map(
        ({
          zaakafhandelParameters,
          bpmnZaakafhandelParametersList,
          bpmnProcessDefinitionsList,
          bmpnFeatureFlag,
        }) => {
          const bpmnZaakafhandelParameters =
            bpmnZaakafhandelParametersList?.find(
              (item) =>
                item.zaaktypeUuid === zaakafhandelParameters.zaaktype.uuid,
            );
          const isBpmn = !!bpmnZaakafhandelParameters; // true if there is a matching BPMN process definition
          const isSavedZaakafhandelParameters =
            isBpmn || !!zaakafhandelParameters?.defaultGroepId; // true if zaakafhandelparameters or BPMN zaakafhandelparameters for this zaaktype has been saved before (id is set on save)

          return {
            zaakafhandelParameters, // CMMN zaakafhandelparameters of this zaaktype
            bpmnProcessDefinitionsList, // BPMN process definitions
            bpmnZaakafhandelParametersList, // BPMN zaak afhandelparameters of this zaaktype
            bpmnZaakafhandelParameters: {
              ...bpmnZaakafhandelParameters,
              zaaktype: zaakafhandelParameters.zaaktype, // will in future be put in by endpoint in backend PR!
            },
            bmpnFeatureFlag,
            isBpmn,
            isSavedZaakafhandelParameters,
          };
        },
      ),
    );
  }
}
