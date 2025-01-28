/*
 * SPDX-FileCopyrightText: 2021-2022 Atos, 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, EventEmitter, Input, OnInit, Output } from "@angular/core";
import { FormControl } from "@angular/forms";

@Component({
  selector: "zac-tekst-filter",
  templateUrl: "./tekst-filter.component.html",
  styleUrls: ["./tekst-filter.component.less"],
})
export class TekstFilterComponent implements OnInit {
  formControl = new FormControl<string>(undefined);
  @Input() value: string;
  @Output() changed = new EventEmitter<string>();

  ngOnInit(): void {
    this.formControl.setValue(this.value);
  }

  change(): void {
    if (this.value !== this.formControl.value) {
      this.value = this.formControl.value;
      this.changed.emit(this.value);
    }
  }
}
