/*
 * SPDX-FileCopyrightText: 2021-2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NgIf } from "@angular/common";
import {
  Component,
  EventEmitter,
  input,
  linkedSignal,
  OnInit,
  Output,
} from "@angular/core";
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
  readonly range = input<GeneratedType<"RestDatumRange"> | undefined>({});
  readonly label = input.required<string>();
  @Output() changed = new EventEmitter<GeneratedType<"RestDatumRange">>();

  protected dateVan = new FormControl<Date | null>(null);
  protected dateTM = new FormControl<Date | null>(null);

  private readonly currentRange = linkedSignal(() => this.range());

  ngOnInit() {
    const range = this.currentRange();
    this.dateVan.setValue(range?.van ? new Date(range.van) : null);
    this.dateTM.setValue(range?.tot ? new Date(range.tot) : null);
  }

  protected change() {
    this.updateRangeProperty("van", this.dateVan);
    this.updateRangeProperty("tot", this.dateTM);
    this.changed.emit(this.currentRange());
  }

  private updateRangeProperty(
    property: "van" | "tot",
    control: FormControl<Date | null>,
  ) {
    const range = this.currentRange();
    if (range?.[property]) {
      range[property] = control.value?.toISOString();
    } else {
      this.currentRange.set({
        ...range,
        [property]: control.value?.toISOString(),
      });
    }
  }

  protected expanded() {
    const range = this.currentRange();
    return !!range?.van || !!range?.tot;
  }
}
