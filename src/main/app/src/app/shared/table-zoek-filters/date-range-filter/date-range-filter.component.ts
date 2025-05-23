/*
 * SPDX-FileCopyrightText: 2021-2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  Component,
  EventEmitter,
  Input,
  OnChanges,
  Output,
} from "@angular/core";
import { FormControl } from "@angular/forms";
import { DatumRange } from "../../../zoeken/model/datum-range";

@Component({
  selector: "zac-date-range-filter",
  templateUrl: "./date-range-filter.component.html",
  styleUrls: ["./date-range-filter.component.less"],
})
export class DateRangeFilterComponent implements OnChanges {
  @Input() range: DatumRange;
  @Input() label: string;
  @Output() changed = new EventEmitter<DatumRange>();

  dateVan = new FormControl<Date>(null);
  dateTM = new FormControl<Date>(null);

  ngOnChanges(): void {
    if (!this.range) {
      this.range = new DatumRange();
    }
    this.dateVan.setValue(this.range.van);
    this.dateTM.setValue(this.range.tot);
  }

  clearDate($event: MouseEvent): void {
    $event.stopPropagation();
    this.dateVan.setValue(null);
    this.dateTM.setValue(null);
    this.range.van = null;
    this.range.tot = null;
    this.changed.emit(this.range);
  }

  change(): void {
    this.range.van = this.dateVan.value;
    this.range.tot = this.dateTM.value;
    if (this.hasRange()) {
      this.changed.emit(this.range);
    }
  }

  hasRange(): boolean {
    if (this.range) {
      return this.range.van != null && this.range.tot != null;
    }
    return false;
  }
}
