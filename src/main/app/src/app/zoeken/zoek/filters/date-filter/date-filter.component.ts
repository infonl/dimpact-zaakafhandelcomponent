/*
 * SPDX-FileCopyrightText: 2021-2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  Component,
  input,
  linkedSignal,
  OnInit,
  output,
  signal,
} from "@angular/core";
import { FormControl, ReactiveFormsModule } from "@angular/forms";
import { MatDatepickerModule } from "@angular/material/datepicker";
import { MatExpansionModule } from "@angular/material/expansion";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatIconModule } from "@angular/material/icon";
import { MatInputModule } from "@angular/material/input";
import { TranslatePipe } from "@ngx-translate/core";
import { fromApiDate, PickedDate, toApiDate } from "../../../../date-time";
import { trackControl } from "../../../../shared/form/control-state";
import { GeneratedType } from "../../../../shared/utils/generated-types";

@Component({
  selector: "zac-date-filter",
  templateUrl: "./date-filter.component.html",
  styleUrls: ["./date-filter.component.less"],
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatExpansionModule,
    MatFormFieldModule,
    MatInputModule,
    MatDatepickerModule,
    MatIconModule,
    TranslatePipe,
  ],
})
export class DateFilterComponent implements OnInit {
  readonly range = input<GeneratedType<"RestDateRange"> | undefined>({});
  readonly label = input.required<string>();
  readonly changed = output<GeneratedType<"RestDateRange">>();

  protected startDate = new FormControl<PickedDate | null>(null);
  protected endDate = new FormControl<PickedDate | null>(null);
  protected readonly hasStartDate = trackControl(signal(this.startDate))(
    (control) => !!control.value,
  );
  protected readonly hasEndDate = trackControl(signal(this.endDate))(
    (control) => !!control.value,
  );

  private readonly currentRange = linkedSignal(() => this.range() ?? {});

  ngOnInit() {
    const range = this.currentRange();
    this.startDate.setValue(fromApiDate(range?.from));
    this.endDate.setValue(fromApiDate(range?.through));
  }

  protected change() {
    this.updateRangeProperty("from", this.startDate);
    this.updateRangeProperty("through", this.endDate);
    this.changed.emit(this.currentRange());
  }

  private updateRangeProperty(
    property: "from" | "through",
    control: FormControl<PickedDate | null>,
  ) {
    const range = this.currentRange();
    if (range?.[property]) {
      range[property] = toApiDate(control.value) ?? undefined;
    } else {
      this.currentRange.set({
        ...range,
        [property]: toApiDate(control.value) ?? undefined,
      });
    }
  }

  protected expanded() {
    const range = this.currentRange();
    return !!range?.from || !!range?.through;
  }
}
