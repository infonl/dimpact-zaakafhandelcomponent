/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Injectable } from "@angular/core";
import { ActivatedRouteSnapshot } from "@angular/router";
import { forkJoin, map } from "rxjs";
import { ConfiguratieService } from "../configuratie/configuratie.service";
import { BpmnService } from "./bpmn.service";
import { ZaakafhandelParametersService } from "./zaakafhandel-parameters.service";

@Injectable({
  providedIn: "root",
})
export class ZaakafhandelParametersResolver {
  constructor(
    private readonly zaakafhandelParametersService: ZaakafhandelParametersService,
    private readonly bpmnService: BpmnService,
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
      bpmnProcessConfigurations:
        this.bpmnService.listbpmnProcessConfigurations(),
      bpmnProcessDefinitions: this.bpmnService.listProcessDefinitions(),
      featureFlagBpmnSupport:
        this.configuratieService.readFeatureFlagBpmnSupport(),
    }).pipe(
      map(
        ({
          zaakafhandelParameters,
          bpmnProcessConfigurations,
          bpmnProcessDefinitions,
          featureFlagBpmnSupport,
        }) => {
          const bpmnZaakafhandelParameters = bpmnProcessConfigurations?.find(
            (item) =>
              item.zaaktypeUuid === zaakafhandelParameters.zaaktype.uuid,
          );
          const isBpmn = !!bpmnZaakafhandelParameters; // true if there is a matching BPMN process definition
          const isSavedZaakafhandelParameters =
            isBpmn || !!zaakafhandelParameters?.defaultGroepId; // true if zaakafhandelparameters or BPMN zaakafhandelparameters for this zaaktype has been saved before (id is set on save)

          return {
            zaakafhandelParameters, // CMMN zaakafhandelparameters of this zaaktype
            bpmnZaakafhandelParameters: {
              ...bpmnZaakafhandelParameters,
              zaaktype: zaakafhandelParameters.zaaktype,
            },
            bpmnProcessDefinitions, // BPMN process definitions
            isBpmn,
            isSavedZaakafhandelParameters,
            featureFlagBpmnSupport,
          };
        },
      ),
    );
  }
}
