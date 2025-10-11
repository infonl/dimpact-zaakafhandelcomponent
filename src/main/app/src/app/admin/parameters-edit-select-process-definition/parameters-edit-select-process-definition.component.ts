/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, EventEmitter, Output } from "@angular/core";
import { FormBuilder, Validators } from "@angular/forms";
import { ActivatedRoute } from "@angular/router";
import {
  ZaakProcessDefinition,
  ZaakProcessSelect,
} from "../model/parameters/zaak-process-definition-type";

@Component({
  selector: "zac-parameters-edit-select-process-definition",
  templateUrl: "./parameters-edit-select-process-definition.component.html",
})
export class ParameterEditSelectProcessDefinitionComponent {
  @Output() switchProcessDefinition = new EventEmitter<ZaakProcessDefinition>();

  protected isBpmnSupported: boolean = false;

  protected readonly zaakProcessDefinitionOptions: Array<{
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
      this.isBpmnSupported = data.parameters.isBpmnSupported;

      if (!this.isBpmnSupported) {
        // if no bpmn support, set input to only possible value and disable it for better UX
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
