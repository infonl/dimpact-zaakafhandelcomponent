/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, inject, input } from "@angular/core";
import { MatCardModule } from "@angular/material/card";
import { TranslatePipe } from "@ngx-translate/core";
import { injectQuery } from "@tanstack/angular-query-experimental";
import { trackControl } from "../../../../shared/form/control-state";
import { ZacSelect } from "../../../../shared/form/select/select";
import { ZacToggle } from "../../../../shared/form/toggle/toggle";
import { ReferentieTabelService } from "../../../referentie-tabel.service";
import { KoppelingenForm } from "./koppelingen-form";

@Component({
  selector: "zac-parameters-step-koppelingen",
  templateUrl: "./parameters-step-koppelingen.component.html",
  standalone: true,
  imports: [MatCardModule, TranslatePipe, ZacSelect, ZacToggle],
})
export class ParametersStepKoppelingenComponent {
  readonly form = input.required<KoppelingenForm>();
  readonly brpDoelbindingSetupEnabled = input.required<boolean>();

  private readonly readForm = trackControl(this.form);
  protected readonly isBrpKoppelen = this.readForm(
    ({ controls }) => controls.brpKoppelen.value,
  );

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
