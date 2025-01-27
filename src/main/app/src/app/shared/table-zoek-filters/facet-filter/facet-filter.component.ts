/*
 * SPDX-FileCopyrightText: 2021-2022 Atos
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
import { FilterParameters } from "../../../zoeken/model/filter-parameters";
import { FilterResultaat } from "../../../zoeken/model/filter-resultaat";

@Component({
  selector: "zac-facet-filter",
  templateUrl: "./facet-filter.component.html",
  styleUrls: ["./facet-filter.component.less"],
})
export class FacetFilterComponent implements OnInit, OnChanges {
  selected = new FormControl<string>(undefined);
  @Input() filter: FilterParameters;
  @Input() opties: FilterResultaat[];
  @Input() label: string;
  @Output() changed = new EventEmitter<FilterParameters>();

  /* veld: prefix */
  public VERTAALBARE_FACETTEN = {
    indicaties: "indicatie.",
    vertrouwelijkheidaanduiding: "vertrouwelijkheidaanduiding.",
    archiefNominatie: "archiefNominatie.",
  };

  getFilters(): FilterResultaat[] {
    if (this.opties) {
      return this.opties.sort((a, b) => a.naam?.localeCompare(b.naam));
    }
  }

  ngOnInit(): void {
    this.setSelected();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.filter && !changes.filter.firstChange) {
      this.setSelected();
    }
  }

  private setSelected(): void {
    this.selected.setValue(this.filter?.values ? this.filter.values[0] : null);
  }

  isVertaalbaar(veld: string): boolean {
    return this.VERTAALBARE_FACETTEN[veld] !== undefined;
  }

  change() {
    this.changed.emit(
      new FilterParameters(
        this.selected.value ? [this.selected.value] : [],
        false,
      ),
    );
  }
}
