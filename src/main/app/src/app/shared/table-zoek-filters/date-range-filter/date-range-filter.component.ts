/*
 * SPDX-FileCopyrightText: 2021-2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NgIf } from "@angular/common";
import { Component, EventEmitter, Input, Output } from "@angular/core";
import { FormControl, ReactiveFormsModule } from "@angular/forms";
import { MAT_DATE_FORMATS } from "@angular/material/core";
import { MatDatepickerModule } from "@angular/material/datepicker";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatIconModule } from "@angular/material/icon";
import { TranslateModule } from "@ngx-translate/core";
import { CapitalizeFirstLetterPipe } from "../../pipes/capitalizeFirstLetter.pipe";
import { GeneratedType } from "../../utils/generated-types";

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
    TranslateModule,
    CapitalizeFirstLetterPipe,
  ],
  providers: [
    {
      provide: MAT_DATE_FORMATS,
      useValue: {
        parse: {
          dateInput: "DD-MM-yyyy",
        },
        display: {
          dateInput: "DD-MM-yyyy",
          monthYearLabel: "MMMM YYYY",
          dateA11yLabel: "LL",
          monthYearA11yLabel: "MMMM YYYY",
        },
      },
    },
  ],
})
export class DateRangeFilterComponent {
  @Input({ required: true })
  set range(value: GeneratedType<"RestDatumRange"> | null | undefined) {
    this.currentRange = value ?? { van: null, tot: null };
    this.dateVan.setValue(this.toDate(this.currentRange.van));
    this.dateTM.setValue(this.toDate(this.currentRange.tot));
  }

  get range() {
    return this.currentRange;
  }

  @Input() label!: string;
  @Input() showLabel?: boolean;
  @Output() changed = new EventEmitter<GeneratedType<"RestDatumRange">>();

  protected dateVan = new FormControl<Date | null>(null);
  protected dateTM = new FormControl<Date | null>(null);

  private currentRange: GeneratedType<"RestDatumRange"> = {
    van: null,
    tot: null,
  };

  protected clearDate($event: MouseEvent): void {
    $event.stopPropagation();
    this.dateVan.setValue(null);
    this.dateTM.setValue(null);
    this.currentRange.van = null;
    this.currentRange.tot = null;
    this.changed.emit(this.currentRange);
  }

  protected change(): void {
    this.currentRange.van = this.dateVan.value?.toISOString() ?? null;
    this.currentRange.tot = this.dateTM.value?.toISOString() ?? null;
    if (this.hasRange()) {
      this.changed.emit(this.currentRange);
    }
  }

  protected hasRange(): boolean {
    return this.currentRange.van != null && this.currentRange.tot != null;
  }

  private toDate(value?: string | null) {
    return value ? new Date(value) : null;
  }
}
