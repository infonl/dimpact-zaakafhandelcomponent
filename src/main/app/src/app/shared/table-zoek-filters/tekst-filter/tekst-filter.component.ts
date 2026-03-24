/*
 * SPDX-FileCopyrightText: 2021-2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, EventEmitter, Input, OnInit, Output } from "@angular/core";
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
export class TekstFilterComponent implements OnInit {
  protected formControl = new FormControl<string | undefined>(undefined);
  @Input() value: string = "";
  @Output() changed = new EventEmitter<string | undefined>();

  ngOnInit(): void {
    this.formControl.setValue(this.value);
  }

  protected change(): void {
    if (this.value !== this.formControl.value) {
      this.value = this.formControl.value ?? "";
      this.changed.emit(this.value);
    }
  }
}
