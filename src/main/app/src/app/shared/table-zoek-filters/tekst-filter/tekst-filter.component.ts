/*
 * SPDX-FileCopyrightText: 2021-2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, effect, model, output, untracked } from "@angular/core";
import { FormControl, ReactiveFormsModule } from "@angular/forms";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatIconModule } from "@angular/material/icon";
import { MatInputModule } from "@angular/material/input";

@Component({
  selector: "zac-tekst-filter",
  templateUrl: "./tekst-filter.component.html",
  styleUrls: ["./tekst-filter.component.less"],
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatIconModule,
  ],
})
export class TekstFilterComponent {
  protected formControl = new FormControl<string | undefined>(undefined);
  readonly value = model<string | null>();
  readonly changed = output<string>();

  constructor() {
    effect(() => {
      const value = this.value() ?? "";
      untracked(() => this.formControl.setValue(value));
    });
  }

  protected change(): void {
    if (this.value() !== this.formControl.value) {
      const value = this.formControl.value ?? "";
      this.value.set(value);
      this.changed.emit(value);
    }
  }
}
