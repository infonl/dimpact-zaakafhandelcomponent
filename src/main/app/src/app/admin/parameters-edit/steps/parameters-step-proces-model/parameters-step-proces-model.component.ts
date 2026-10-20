/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, computed, inject, input } from "@angular/core";
import { MatIconModule } from "@angular/material/icon";
import { TranslateModule } from "@ngx-translate/core";
import { injectQuery } from "@tanstack/angular-query-experimental";
import { ZacSelect } from "../../../../shared/form/select/select";
import { ZaakafhandelParametersService } from "../../../zaakafhandel-parameters.service";
import { ProcessDefinition, ProcessModelForm } from "./process-model-form";

@Component({
  selector: "zac-parameters-step-proces-model",
  templateUrl: "./parameters-step-proces-model.component.html",
  standalone: true,
  imports: [MatIconModule, TranslateModule, ZacSelect],
})
export class ParametersStepProcesModelComponent {
  readonly form = input.required<ProcessModelForm>();
  readonly zaaktypeOmschrijving = input<string | null>();
  readonly savedProcessDefinition = input<ProcessDefinition | null>(null);

  private readonly zaakafhandelParametersService = inject(
    ZaakafhandelParametersService,
  );
  private readonly processDefinitionsQuery = injectQuery(() =>
    this.zaakafhandelParametersService.listProcessDefinitions(),
  );

  /** A saved configuration never changes process engine, so it only offers the process definitions of its engine. */
  protected readonly processDefinitions = computed(() => {
    const savedProcessEngine = this.savedProcessDefinition()?.processEngine;
    return (this.processDefinitionsQuery.data() ?? []).filter(
      ({ processEngine }) =>
        !savedProcessEngine || processEngine === savedProcessEngine,
    );
  });

  protected readonly displayName = (processDefinition: ProcessDefinition) =>
    processDefinition.naam ?? processDefinition.key;

  protected readonly isSameProcessDefinition = (
    processDefinition: ProcessDefinition,
    other?: ProcessDefinition | null,
  ) =>
    processDefinition.processEngine === other?.processEngine &&
    processDefinition.key === other?.key;
}
