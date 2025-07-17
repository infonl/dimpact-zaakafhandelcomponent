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
    if (this.range?.van) this.range.van = this.dateVan.value?.toISOString();
    else
      this.range = {
        van: this.dateVan.value?.toISOString(),
        tot: this.range?.tot,
      };

    if (this.range?.tot) this.range.tot = this.dateTM.value?.toISOString();
    else
      this.range = {
        tot: this.dateTM.value?.toISOString(),
        van: this.range?.van,
      };
    this.changed.emit(this.range);
  }

  expanded() {
    return !!this.range?.van || !!this.range?.tot;
  }
}
