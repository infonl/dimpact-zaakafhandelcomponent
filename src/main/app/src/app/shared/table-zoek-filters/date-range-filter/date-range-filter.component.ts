/*
 * SPDX-FileCopyrightText: 2021-2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  Component,
  computed,
  effect,
  input,
  model,
  output,
  untracked,
} from "@angular/core";
import { FormControl, ReactiveFormsModule } from "@angular/forms";
import { MatDatepickerModule } from "@angular/material/datepicker";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatIconModule } from "@angular/material/icon";
import { TranslatePipe } from "@ngx-translate/core";
import {
  fromApiDate,
  PickedDate,
  provideFilterDateFormats,
  toApiDate,
} from "../../../date-time";
import { CapitalizeFirstLetterPipe } from "../../pipes/capitalizeFirstLetter.pipe";
import { GeneratedType } from "../../utils/generated-types";

@Component({
  selector: "zac-date-range-filter",
  templateUrl: "./date-range-filter.component.html",
  styleUrls: ["./date-range-filter.component.less"],
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatFormFieldModule,
    MatDatepickerModule,
    MatIconModule,
    TranslatePipe,
    CapitalizeFirstLetterPipe,
  ],
  providers: [provideFilterDateFormats()],
})
export class DateRangeFilterComponent {
  readonly range = model.required<
    GeneratedType<"RestDateRange"> | null | undefined
  >();
  readonly label = input.required<string>();
  readonly showLabel = input<boolean>();
  readonly changed = output<GeneratedType<"RestDateRange">>();

  protected startDate = new FormControl<PickedDate | null>(null);
  protected endDate = new FormControl<PickedDate | null>(null);

  private readonly currentRange = computed<GeneratedType<"RestDateRange">>(
    () => this.range() ?? { from: null, through: null },
  );

  constructor() {
    effect(() => {
      const currentRange = this.currentRange();
      untracked(() => {
        this.startDate.setValue(fromApiDate(currentRange.from));
        this.endDate.setValue(fromApiDate(currentRange.through));
      });
    });
  }

  protected clearDate($event: MouseEvent): void {
    $event.stopPropagation();
    this.startDate.setValue(null);
    this.endDate.setValue(null);
    const range = { from: null, through: null };
    this.setRange(range);
    this.changed.emit(range);
  }

  protected change(): void {
    const range = {
      from: toApiDate(this.startDate.value),
      through: toApiDate(this.endDate.value),
    };
    this.setRange(range);
    if (this.hasRange()) {
      this.changed.emit(range);
    }
  }

  protected hasRange(): boolean {
    const currentRange = this.currentRange();
    return currentRange.from != null || currentRange.through != null;
  }

  private setRange(range: GeneratedType<"RestDateRange">) {
    const currentRange = this.currentRange();
    if (
      currentRange.from === range.from &&
      currentRange.through === range.through
    ) {
      return;
    }
    this.range.set(range);
  }
}
