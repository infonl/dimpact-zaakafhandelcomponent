/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, input } from "@angular/core";
import { ReactiveFormsModule } from "@angular/forms";
import { MatExpansionModule } from "@angular/material/expansion";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatInputModule } from "@angular/material/input";
import { TranslateModule } from "@ngx-translate/core";
import { ActiesForm } from "./acties-form";

@Component({
  selector: "zac-parameters-step-acties",
  templateUrl: "./parameters-step-acties.component.html",
  styleUrl: "./parameters-step-acties.component.less",
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatExpansionModule,
    MatFormFieldModule,
    MatInputModule,
    TranslateModule,
  ],
})
export class ParametersStepActiesComponent {
  readonly form = input.required<ActiesForm>();
}
