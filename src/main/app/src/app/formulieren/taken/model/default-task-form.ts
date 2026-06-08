/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Injectable } from "@angular/core";
import { Validators } from "@angular/forms";
import { FormField } from "../../../shared/form/composed-form/form-field.types";
import { GeneratedType } from "../../../shared/utils/generated-types";
import { AbstractTaskForm } from "./abstract-task-form";

@Injectable({
  providedIn: "root",
})
export class DefaultTaskForm extends AbstractTaskForm {
  async requestForm(): Promise<FormField[]> {
    return [
      {
        type: "textarea",
        key: "redenStart",
        control: this.formBuilder.control<string | null>(null, [
          Validators.required,
          Validators.maxLength(1000),
        ]),
      },
    ];
  }

  async handleForm(taak: GeneratedType<"RestTask">): Promise<FormField[]> {
    return [
      {
        type: "plain-text",
        key: "redenStart",
        label: "redenStart",
      },
      {
        type: "textarea",
        key: "afhandeling",
        control: this.formBuilder.control(
          taak.taakdata?.["afhandeling"] ?? null,
          [Validators.required, Validators.maxLength(1000)],
        ),
        readonly: taak.status === "AFGEROND" || !taak.rechten?.wijzigen,
      },
    ];
  }
}
