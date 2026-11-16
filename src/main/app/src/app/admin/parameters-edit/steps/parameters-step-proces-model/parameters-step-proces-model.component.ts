/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, computed, inject, input } from "@angular/core";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatIconModule } from "@angular/material/icon";
import { TranslatePipe } from "@ngx-translate/core";
import { injectQuery } from "@tanstack/angular-query-experimental";
import { ZacSelect } from "../../../../shared/form/select/select";
import { DatumPipe } from "../../../../shared/pipes/datum.pipe";
import { GeneratedType } from "../../../../shared/utils/generated-types";
import { ZaakafhandelParametersService } from "../../../zaakafhandel-parameters.service";
import { ProcessDefinition, ProcessModelForm } from "./process-model-form";

@Component({
  selector: "zac-parameters-step-proces-model",
  templateUrl: "./parameters-step-proces-model.component.html",
  standalone: true,
  imports: [
    DatumPipe,
    MatFormFieldModule,
    MatIconModule,
    TranslatePipe,
    ZacSelect,
  ],
})
export class ParametersStepProcesModelComponent {
  readonly form = input.required<ProcessModelForm>();
  readonly zaaktype = input.required<GeneratedType<"RestZaaktypeOverzicht">>();
  readonly isBound = input(false);
  readonly isProcessDefinitionChangeable = input<boolean | null>();

  private readonly zaakafhandelParametersService = inject(
    ZaakafhandelParametersService,
  );
  private readonly processDefinitionsQuery = injectQuery(() =>
    this.zaakafhandelParametersService.listProcessDefinitions(),
  );

  protected readonly processDefinitions = computed(
    () => this.processDefinitionsQuery.data() ?? [],
  );

  protected readonly isLocked = computed(
    () => this.isProcessDefinitionChangeable() === false,
  );

  protected readonly hint = computed(() => {
    if (this.isLocked()) return "zaps.step.procesmodel.locked";
    if (this.isBound()) return "zaps.step.procesmodel.changeable";
    return null;
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
