/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, output } from "@angular/core";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { MatButtonModule } from "@angular/material/button";
import { MatStepperModule } from "@angular/material/stepper";
import { ActivatedRoute } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { GeneratedType } from "src/app/shared/utils/generated-types";
import { ProcessModelMethodSelection } from "../model/parameters/process-model-method";
import { ParametersStepProcesModelComponent } from "../parameters-edit/steps/parameters-step-proces-model/parameters-step-proces-model.component";
import { createProcessModelForm } from "../parameters-edit/steps/parameters-step-proces-model/process-model-form";

@Component({
  selector: "zac-parameters-select-process-model-method",
  templateUrl: "./parameters-select-process-model-method.component.html",
  standalone: true,
  imports: [
    MatStepperModule,
    MatButtonModule,
    TranslateModule,
    ParametersStepProcesModelComponent,
  ],
})
export class ParameterSelectProcessModelMethodComponent {
  readonly switchModellingMethod = output<ProcessModelMethodSelection>();

  protected caseParameters: GeneratedType<"RestZaaktypeBpmnConfiguration"> & {
    zaaktype: GeneratedType<"RestZaaktype">;
  } = {
    zaaktypeUuid: "",
    zaaktypeOmschrijving: "",
    bpmnProcessDefinitionKey: "",
    productaanvraagtype: null,
    groepNaam: "",
    zaaktype: {
      uuid: "",
      identificatie: "",
      doel: "",
      omschrijving: "",
    },
    betrokkeneKoppelingen: {},
    brpDoelbindingen: {},
    zaakbeeindigParameters: [],
  };

  protected cmmnBpmnFormGroup = createProcessModelForm(null);

  constructor(private readonly route: ActivatedRoute) {
    this.route.data.pipe(takeUntilDestroyed()).subscribe((data) => {
      this.caseParameters = data.parameters.zaakafhandelParameters;
    });

    this.cmmnBpmnFormGroup.controls.options.valueChanges.subscribe((value) => {
      this.switchModellingMethod.emit({ type: value?.value ?? null });
    });
  }
}
