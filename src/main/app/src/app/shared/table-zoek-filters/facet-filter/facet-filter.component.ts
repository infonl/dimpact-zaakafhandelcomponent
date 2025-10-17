/*
 * SPDX-FileCopyrightText: 2021-2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  Component,
  EventEmitter,
  Input,
  OnChanges,
  OnInit,
  Output,
  SimpleChanges,
} from "@angular/core";
import { FormControl } from "@angular/forms";
import { GeneratedType } from "../../utils/generated-types";

@Component({
  selector: "zac-facet-filter",
  templateUrl: "./facet-filter.component.html",
  styleUrls: ["./facet-filter.component.less"],
})
export class FacetFilterComponent implements OnInit, OnChanges {
  selected = new FormControl<string | undefined>(undefined);
  @Input() filter?: GeneratedType<"FilterParameters">;
  @Input() opties?: GeneratedType<"FilterResultaat">[] = [];
  @Input({ required: true }) label!: string;
  @Output() changed = new EventEmitter<GeneratedType<"FilterParameters">>();

  /* veld: prefix */
  public VERTAALBARE_FACETTEN = {
    indicaties: "indicatie.",
    vertrouwelijkheidaanduiding: "vertrouwelijkheidaanduiding.",
    archiefNominatie: "archiefNominatie.",
  };

  getFilters() {
    return this.opties?.sort((a, b) => a.naam.localeCompare(b.naam));
  }

  ngOnInit() {
    this.setSelected();
  }

  ngOnChanges(changes: SimpleChanges) {
    if (changes.filter && !changes.filter.firstChange) {
      this.setSelected();
    }
  }

  private setSelected() {
    this.selected.setValue(this.filter?.values?.[0] ?? null);
  }

  isVertaalbaar(veld: string) {
    return veld in this.VERTAALBARE_FACETTEN;
  }

  change() {
    this.changed.emit({
      values: this.selected.value ? [this.selected.value] : [],
      inverse: false,
    });
  }
}
