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
  readonly range = input<GeneratedType<"RestDatumRange"> | undefined>({});
  readonly label = input.required<string>();
  readonly changed = output<GeneratedType<"RestDatumRange">>();

  protected dateVan = new FormControl<PickedDate | null>(null);
  protected dateTM = new FormControl<PickedDate | null>(null);
  protected readonly hasDateVan = trackControl(signal(this.dateVan))(
    (control) => !!control.value,
  );
  protected readonly hasDateTM = trackControl(signal(this.dateTM))(
    (control) => !!control.value,
  );

  private readonly currentRange = linkedSignal(() => this.range() ?? {});

  ngOnInit() {
    const range = this.currentRange();
    this.dateVan.setValue(fromApiDate(range?.van));
    this.dateTM.setValue(fromApiDate(range?.tot));
  }

  protected change() {
    this.updateRangeProperty("van", this.dateVan);
    this.updateRangeProperty("tot", this.dateTM);
    this.changed.emit(this.currentRange());
  }

  private updateRangeProperty(
    property: "van" | "tot",
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
    return !!range?.van || !!range?.tot;
  }
}
