/*
 * SPDX-FileCopyrightText: 2021-2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NgIf } from "@angular/common";
import {
  Component,
  computed,
  effect,
  EventEmitter,
  input,
  Output,
  untracked,
} from "@angular/core";
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
  readonly range = input.required<
    GeneratedType<"RestDatumRange"> | null | undefined
  >();
  readonly label = input.required<string>();
  readonly showLabel = input<boolean>();
  @Output() changed = new EventEmitter<GeneratedType<"RestDatumRange">>();

  protected dateVan = new FormControl<Date | null>(null);
  protected dateTM = new FormControl<Date | null>(null);

  private readonly currentRange = computed<GeneratedType<"RestDatumRange">>(
    () => this.range() ?? { van: null, tot: null },
  );

  constructor() {
    effect(() => {
      const currentRange = this.currentRange();
      untracked(() => {
        this.dateVan.setValue(this.toDate(currentRange.van));
        this.dateTM.setValue(this.toDate(currentRange.tot));
      });
    });
  }

  protected clearDate($event: MouseEvent): void {
    $event.stopPropagation();
    this.dateVan.setValue(null);
    this.dateTM.setValue(null);
    const currentRange = this.currentRange();
    currentRange.van = null;
    currentRange.tot = null;
    this.changed.emit(currentRange);
  }

  protected change(): void {
    const currentRange = this.currentRange();
    currentRange.van = this.dateVan.value?.toISOString() ?? null;
    currentRange.tot = this.dateTM.value?.toISOString() ?? null;
    if (this.hasRange()) {
      this.changed.emit(currentRange);
    }
  }

  protected hasRange(): boolean {
    const currentRange = this.currentRange();
    return currentRange.van != null || currentRange.tot != null;
  }

  private toDate(value?: string | null) {
    return value ? new Date(value) : null;
  }
}
