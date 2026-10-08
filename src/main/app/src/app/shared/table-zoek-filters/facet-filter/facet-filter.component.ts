/*
 * SPDX-FileCopyrightText: 2021-2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NgFor } from "@angular/common";
import { Component, effect, input, output, untracked } from "@angular/core";
import { FormControl, ReactiveFormsModule } from "@angular/forms";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatSelectModule } from "@angular/material/select";
import { TranslateModule } from "@ngx-translate/core";
import { I18nKeyPipe } from "../../pipes/i18n-key.pipe";
import { GeneratedType } from "../../utils/generated-types";

@Component({
  selector: "zac-facet-filter",
  templateUrl: "./facet-filter.component.html",
  styleUrls: ["./facet-filter.component.less"],
  standalone: true,
  imports: [
    I18nKeyPipe,
    NgFor,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatSelectModule,
    TranslateModule,
  ],
})
export class FacetFilterComponent {
  protected selected = new FormControl<string | undefined>(undefined);
  readonly filter = input<GeneratedType<"FilterParameters">>();
  readonly opties = input<GeneratedType<"FilterResultaat">[] | undefined>([]);
  readonly label = input.required<string>();
  readonly changed = output<GeneratedType<"FilterParameters">>();

  /* veld: prefix */
  protected VERTAALBARE_FACETTEN: Record<string, string> = {
    indicaties: "indicatie.",
    vertrouwelijkheidaanduiding: "vertrouwelijkheidaanduiding.",
    archiefNominatie: "archief-nominatie.",
  };

  constructor() {
    effect(() => {
      const firstValue = this.filter()?.values?.[0] ?? null;
      untracked(() => this.selected.setValue(firstValue));
    });
  }

  protected getFilters() {
    return this.opties()?.sort((a, b) => a.naam.localeCompare(b.naam));
  }

  protected isVertaalbaar(veld: string) {
    return veld in this.VERTAALBARE_FACETTEN;
  }

  protected change() {
    this.changed.emit({
      values: this.selected.value ? [this.selected.value] : [],
      inverse: false,
    });
  }
}
