/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, output } from "@angular/core";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { FormBuilder, Validators } from "@angular/forms";
import { MatButtonModule } from "@angular/material/button";
import { MatIconModule } from "@angular/material/icon";
import { MatStepperModule } from "@angular/material/stepper";
import { ActivatedRoute } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { ZacRadio } from "src/app/shared/form/radio/radio";
import { GeneratedType } from "src/app/shared/utils/generated-types";
import {
  ProcessModelMethod,
  ProcessModelMethodSelection,
} from "../model/parameters/process-model-method";

@Component({
  selector: "zac-parameters-select-process-model-method",
  templateUrl: "./parameters-select-process-model-method.component.html",
  standalone: true,
  imports: [
    MatStepperModule,
    MatIconModule,
    MatButtonModule,
    TranslateModule,
    ZacRadio,
  ],
})
export class ParameterSelectProcessModelMethodComponent {
  readonly switchModellingMethod = output<ProcessModelMethodSelection>();

  protected readonly modellingMethodOptions: Array<{
    label: string;
    value: ProcessModelMethod;
  }> = [
    { label: "CMMN", value: "CMMN" },
    { label: "BPMN", value: "BPMN" },
  ];

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

  protected cmmnBpmnFormGroup = this.formBuilder.group({
    options: this.formBuilder.control<{
      value: ProcessModelMethod;
      label: string;
    } | null>(null, [Validators.required]),
  });

  constructor(
    private readonly formBuilder: FormBuilder,
    private readonly route: ActivatedRoute,
  ) {
    this.route.data.pipe(takeUntilDestroyed()).subscribe((data) => {
      this.caseParameters = data.parameters.zaakafhandelParameters;
    });

    this.cmmnBpmnFormGroup.controls.options.valueChanges.subscribe((value) => {
      this.switchModellingMethod.emit({ type: value?.value ?? null });
    });
  }
}
