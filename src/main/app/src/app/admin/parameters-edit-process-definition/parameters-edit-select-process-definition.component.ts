/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, EventEmitter, Output } from "@angular/core";
import { FormBuilder, Validators } from "@angular/forms";
import { ZaakProcessDefinition } from "../model/parameters/parameters-edit-process-definition-type";

@Component({
  selector: "zac-parameters-edit-select-process-definition",
  templateUrl: "./parameters-edit-select-process-definition.component.html",
})
export class ParameterEditSelectProcessDefinitionComponent {
  @Output() switchProcessDefinition = new EventEmitter<ZaakProcessDefinition>();

  constructor(private readonly formBuilder: FormBuilder) {}

  cmmnBpmnFormGroup = this.formBuilder.group({
    options: this.formBuilder.control<{
      value: string;
      label: string;
    } | null>(null, [Validators.required]),
  });
}
