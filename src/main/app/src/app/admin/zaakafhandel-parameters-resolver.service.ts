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
      featureFlagPabcIntegration:
        this.configuratieService.readFeatureFlagPabcIntegration(),
    }).pipe(
      map(
        ({
          zaakafhandelParameters,
          bpmnProcessConfigurations,
          bpmnProcessDefinitions,
          featureFlagPabcIntegration,
        }) => {
          const bpmnZaakafhandelParameters = bpmnProcessConfigurations?.find(
            (item) =>
              item.zaaktypeUuid === zaakafhandelParameters.zaaktype.uuid,
          );
          const isBpmn = !!bpmnZaakafhandelParameters;
          const isSavedZaakafhandelParameters =
            isBpmn || !!zaakafhandelParameters?.defaultGroepId; // true if zaakafhandelparameters or BPMN zaakafhandelparameters for this zaaktype has been saved before (group id initially null but set on save)

          return {
            zaakafhandelParameters,
            bpmnZaakafhandelParameters: {
              ...bpmnZaakafhandelParameters,
              zaaktype: zaakafhandelParameters.zaaktype,
              zaakbeeindigParameters: [],
            },
            bpmnProcessDefinitions,
            isBpmn,
            isSavedZaakafhandelParameters,
            featureFlagPabcIntegration,
          };
        },
      ),
    );
  }
}
