/*
 * SPDX-FileCopyrightText: 2021-2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NgIf } from "@angular/common";
import {
  Component,
  EventEmitter,
  Input,
  OnChanges,
  Output,
} from "@angular/core";
import { FormControl, ReactiveFormsModule } from "@angular/forms";
import { MatDatepickerModule } from "@angular/material/datepicker";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatIconModule } from "@angular/material/icon";
import { DatumRange } from "../../../zoeken/model/datum-range";

@Component({
  selector: "zac-date-range-filter",
  templateUrl: "./date-range-filter.component.html",
  styleUrls: ["./date-range-filter.component.less"],
  standalone: true,
  imports: [
    NgIf,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatDatepickerModule,
    MatIconModule,
  ],
})
export class DateRangeFilterComponent implements OnChanges {
  @Input({ required: true }) range!: DatumRange;
  @Input() label!: string;
  @Output() changed = new EventEmitter<DatumRange>();

  protected dateVan = new FormControl<Date | null>(null);
  protected dateTM = new FormControl<Date | null>(null);

  ngOnChanges(): void {
    if (!this.range) {
      this.range = new DatumRange();
    }
    this.dateVan.setValue(this.range.van);
    this.dateTM.setValue(this.range.tot);
  }

  protected clearDate($event: MouseEvent): void {
    $event.stopPropagation();
    this.dateVan.setValue(null);
    this.dateTM.setValue(null);
    this.range.van = null;
    this.range.tot = null;
    this.changed.emit(this.range);
  }

  protected change(): void {
    this.range.van = this.dateVan.value;
    this.range.tot = this.dateTM.value;
    if (this.hasRange()) {
      this.changed.emit(this.range);
    }
  }

  protected hasRange(): boolean {
    if (this.range) {
      return this.range.van != null && this.range.tot != null;
    }
    return false;
  }
}
