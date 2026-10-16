/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, input } from "@angular/core";
import { MatIconModule } from "@angular/material/icon";
import { TranslateModule } from "@ngx-translate/core";
import { ZacRadio } from "../../../../shared/form/radio/radio";
import {
  ProcessModelForm,
  processModelMethodOptions,
} from "./process-model-form";

@Component({
  selector: "zac-parameters-step-proces-model",
  templateUrl: "./parameters-step-proces-model.component.html",
  standalone: true,
  imports: [MatIconModule, TranslateModule, ZacRadio],
})
export class ParametersStepProcesModelComponent {
  readonly form = input.required<ProcessModelForm>();
  readonly zaaktypeOmschrijving = input<string | null>();

  protected readonly options = processModelMethodOptions;
}
