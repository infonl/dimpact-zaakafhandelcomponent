/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, EventEmitter, Output } from "@angular/core";
import { FormBuilder, Validators } from "@angular/forms";
import {
  ZaakProcessDefinition,
  ZaakProcessSelect,
} from "../model/parameters/zaak-process-definition-type";
import { ActivatedRoute } from "@angular/router";

@Component({
  selector: "zac-parameters-edit-select-process-definition",
  templateUrl: "./parameters-edit-select-process-definition.component.html",
})
export class ParameterEditSelectProcessDefinitionComponent {
  @Output() switchProcessDefinition = new EventEmitter<ZaakProcessDefinition>();

  protected bmpnFeatureFlag: boolean = false;

  protected readonly zaakProcessDefinitions: Array<{
    label: string;
    value: ZaakProcessSelect;
  }> = [
    { label: "CMMN", value: "CMMN" },
    { label: "BPMN", value: "BPMN" },
  ];

  cmmnBpmnFormGroup = this.formBuilder.group({
    options: this.formBuilder.control<{
      value: ZaakProcessSelect;
      label: string;
    } | null>(null, [Validators.required]),
  });

  constructor(
    private readonly formBuilder: FormBuilder,
    private readonly route: ActivatedRoute,
  ) {
    this.route.data.subscribe(async (data) => {
      this.bmpnFeatureFlag = data.parameters.bmpnFeatureFlag;

      if (!this.bmpnFeatureFlag) {
        this.cmmnBpmnFormGroup.controls.options.setValue({
          value: "CMMN",
          label: "CMMN",
        });
        this.cmmnBpmnFormGroup.controls.options.disable();
        this.cmmnBpmnFormGroup.controls.options.setValidators([]);
        this.cmmnBpmnFormGroup.updateValueAndValidity();
      }
    });
  }

  protected onNext() {
    const selectedOption = this.cmmnBpmnFormGroup.value.options?.value;
    if (selectedOption) {
      this.switchProcessDefinition.emit({
        type: selectedOption,
        selectedIndexStart: 1,
      });
    }
  }
}
