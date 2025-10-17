/*
 * SPDX-FileCopyrightText: 2021-2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, EventEmitter, Input, OnInit, Output } from "@angular/core";
import { FormControl } from "@angular/forms";
import { GeneratedType } from "../../../../shared/utils/generated-types";

@Component({
  selector: "zac-date-filter",
  templateUrl: "./date-filter.component.html",
  styleUrls: ["./date-filter.component.less"],
})
export class DateFilterComponent implements OnInit {
  @Input() range?: GeneratedType<"RestDatumRange"> = {};
  @Input({ required: true }) label!: string;
  @Output() changed = new EventEmitter<GeneratedType<"RestDatumRange">>();

  dateVan = new FormControl<Date | null>(null);
  dateTM = new FormControl<Date | null>(null);

  ngOnInit() {
    this.dateVan.setValue(this.range?.van ? new Date(this.range.van) : null);
    this.dateTM.setValue(this.range?.tot ? new Date(this.range.tot) : null);
  }

  change() {
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

  expanded() {
    return !!this.range?.van || !!this.range?.tot;
  }
}
