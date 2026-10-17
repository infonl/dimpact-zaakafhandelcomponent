/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { FormControl, FormGroup, Validators } from "@angular/forms";
import { ProcessModelMethod } from "../../../model/parameters/process-model-method";

export type ProcessModelMethodOption = {
  label: ProcessModelMethod;
  value: ProcessModelMethod;
};

export const processModelMethodOptions: ProcessModelMethodOption[] = [
  { label: "CMMN", value: "CMMN" },
  { label: "BPMN", value: "BPMN" },
];

export type ProcessModelForm = FormGroup<{
  options: FormControl<ProcessModelMethodOption | null>;
}>;

export function createProcessModelForm(
  method: ProcessModelMethod | null,
): ProcessModelForm {
  return new FormGroup({
    options: new FormControl<ProcessModelMethodOption | null>(
      method ? { label: method, value: method } : null,
      [Validators.required],
    ),
  });
}
