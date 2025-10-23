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

@Component({
  selector: "zac-parameters-edit-select-process-definition",
  templateUrl: "./parameters-edit-select-process-definition.component.html",
})
export class ParameterEditSelectProcessDefinitionComponent {
  @Output() switchProcessDefinition = new EventEmitter<ZaakProcessDefinition>();

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
    } | null>(null, []),
  });

  constructor(private readonly formBuilder: FormBuilder) {
    this.cmmnBpmnFormGroup.controls.options.valueChanges.subscribe((value) => {
      this.switchProcessDefinition.emit({
        type: value?.value || "SELECT-PROCESS-DEFINITION",
      });
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
