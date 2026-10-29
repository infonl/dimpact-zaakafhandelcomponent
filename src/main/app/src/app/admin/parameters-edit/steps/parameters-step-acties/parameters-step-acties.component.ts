/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, input, ChangeDetectionStrategy } from "@angular/core";
import { MatExpansionModule } from "@angular/material/expansion";
import { ZacTextarea } from "../../../../shared/form/textarea/textarea";
import { ActiesForm } from "./acties-form";

@Component({
  selector: "zac-parameters-step-acties",
  templateUrl: "./parameters-step-acties.component.html",
  standalone: true,
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [MatExpansionModule, ZacTextarea],
})
export class ParametersStepActiesComponent {
  readonly form = input.required<ActiesForm>();
}
