/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { FormControl, FormGroup, Validators } from "@angular/forms";
import { GeneratedType } from "../../../../shared/utils/generated-types";

export type ProcessDefinition = GeneratedType<"RestProcessDefinition">;

export type ProcessModelForm = FormGroup<{
  processDefinition: FormControl<ProcessDefinition | null>;
}>;

export function createProcessModelForm(
  processDefinition: ProcessDefinition | null,
): ProcessModelForm {
  return new FormGroup({
    processDefinition: new FormControl<ProcessDefinition | null>(
      processDefinition,
      [Validators.required],
    ),
  });
}

export function toProcessDefinition(form: ProcessModelForm) {
  return form.controls.processDefinition.value;
}
