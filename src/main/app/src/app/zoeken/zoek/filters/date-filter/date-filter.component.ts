/*
 * SPDX-FileCopyrightText: 2021-2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NgIf } from "@angular/common";
import { Component, EventEmitter, Input, OnInit, Output } from "@angular/core";
import { FormControl, ReactiveFormsModule } from "@angular/forms";
import { MatDatepickerModule } from "@angular/material/datepicker";
import { MatExpansionModule } from "@angular/material/expansion";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatIconModule } from "@angular/material/icon";
import { MatInputModule } from "@angular/material/input";
import { TranslateModule } from "@ngx-translate/core";
import { GeneratedType } from "../../../../shared/utils/generated-types";

@Component({
  selector: "zac-date-filter",
  templateUrl: "./date-filter.component.html",
  styleUrls: ["./date-filter.component.less"],
  standalone: true,
  imports: [
    NgIf,
    ReactiveFormsModule,
    MatExpansionModule,
    MatFormFieldModule,
    MatInputModule,
    MatDatepickerModule,
    MatIconModule,
    TranslateModule,
  ],
})
export class DateFilterComponent implements OnInit {
  @Input() range?: GeneratedType<"RestDatumRange"> = {};
  @Input({ required: true }) label!: string;
  @Output() changed = new EventEmitter<GeneratedType<"RestDatumRange">>();

  protected dateVan = new FormControl<Date | null>(null);
  protected dateTM = new FormControl<Date | null>(null);

  ngOnInit() {
    this.dateVan.setValue(this.range?.van ? new Date(this.range.van) : null);
    this.dateTM.setValue(this.range?.tot ? new Date(this.range.tot) : null);
  }

  protected change() {
    this.updateRangeProperty("van", this.dateVan);
    this.updateRangeProperty("tot", this.dateTM);
    this.changed.emit(this.range);
  }

  private updateRangeProperty(
    property: "van" | "tot",
    control: FormControl<Date | null>,
  ) {
    if (this.range?.[property]) {
      this.range[property] = control.value?.toISOString();
    } else {
      this.range = {
        ...this.range,
        [property]: control.value?.toISOString(),
      };
    }
  }

  protected expanded() {
    return !!this.range?.van || !!this.range?.tot;
  }
}
