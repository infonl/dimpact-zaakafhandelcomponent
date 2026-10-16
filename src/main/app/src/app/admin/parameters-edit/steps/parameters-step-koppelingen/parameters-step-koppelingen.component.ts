/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, inject, input } from "@angular/core";
import { ReactiveFormsModule } from "@angular/forms";
import { MatCardModule } from "@angular/material/card";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatSelectModule } from "@angular/material/select";
import { MatSlideToggleModule } from "@angular/material/slide-toggle";
import { TranslateModule } from "@ngx-translate/core";
import { injectQuery } from "@tanstack/angular-query-experimental";
import { ReferentieTabelService } from "../../../referentie-tabel.service";
import { KoppelingenForm } from "./koppelingen-form";

@Component({
  selector: "zac-parameters-step-koppelingen",
  templateUrl: "./parameters-step-koppelingen.component.html",
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatSelectModule,
    MatSlideToggleModule,
    TranslateModule,
  ],
})
export class ParametersStepKoppelingenComponent {
  readonly form = input.required<KoppelingenForm>();
  readonly brpDoelbindingSetupEnabled = input.required<boolean>();

  private readonly referentieTabelService = inject(ReferentieTabelService);

  protected readonly brpSearchValuesQuery = injectQuery(() =>
    this.referentieTabelService.listBrpSearchValues(),
  );
  protected readonly brpViewValuesQuery = injectQuery(() =>
    this.referentieTabelService.listBrpViewValues(),
  );
  protected readonly brpProcessingValuesQuery = injectQuery(() =>
    this.referentieTabelService.listBrpProcessingValues(),
  );
}
