/*
 * SPDX-FileCopyrightText: 2021-2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NgFor } from "@angular/common";
import {
  Component,
  EventEmitter,
  Input,
  OnChanges,
  OnInit,
  Output,
  SimpleChanges,
} from "@angular/core";
import { FormControl, ReactiveFormsModule } from "@angular/forms";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatSelectModule } from "@angular/material/select";
import { TranslateModule } from "@ngx-translate/core";
import { GeneratedType } from "../../utils/generated-types";

@Component({
  selector: "zac-facet-filter",
  templateUrl: "./facet-filter.component.html",
  styleUrls: ["./facet-filter.component.less"],
  standalone: true,
  imports: [
    NgFor,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatSelectModule,
    TranslateModule,
  ],
})
export class FacetFilterComponent implements OnInit, OnChanges {
  protected selected = new FormControl<string | undefined>(undefined);
  @Input() filter?: GeneratedType<"FilterParameters">;
  @Input() opties?: GeneratedType<"FilterResultaat">[] = [];
  @Input({ required: true }) label!: string;
  @Output() changed = new EventEmitter<GeneratedType<"FilterParameters">>();

  /* veld: prefix */
  protected VERTAALBARE_FACETTEN: Record<string, string> = {
    indicaties: "indicatie.",
    vertrouwelijkheidaanduiding: "vertrouwelijkheidaanduiding.",
    archiefNominatie: "archiefNominatie.",
  };

  protected getFilters() {
    return this.opties?.sort((a, b) => a.naam.localeCompare(b.naam));
  }

  ngOnInit() {
    this.setSelected();
  }

  ngOnChanges(changes: SimpleChanges) {
    if (changes["filter"] && !changes["filter"].firstChange) {
      this.setSelected();
    }
  }

  private setSelected() {
    this.selected.setValue(this.filter?.values?.[0] ?? null);
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
