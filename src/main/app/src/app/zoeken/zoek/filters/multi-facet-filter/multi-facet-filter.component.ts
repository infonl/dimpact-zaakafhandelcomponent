/*
 * SPDX-FileCopyrightText: 2021-2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, EventEmitter, Input, OnInit, Output } from "@angular/core";
import { FormBuilder, FormControl } from "@angular/forms";
import { GeneratedType } from "../../../../shared/utils/generated-types";

@Component({
  selector: "zac-multi-facet-filter",
  templateUrl: "./multi-facet-filter.component.html",
  styleUrls: ["./multi-facet-filter.component.less"],
})
export class MultiFacetFilterComponent implements OnInit {
  @Input({ required: true }) filter!: GeneratedType<"FilterParameters">;
  @Input({ required: true }) opties!: GeneratedType<"FilterResultaat">[];
  @Input({ required: true }) label!: string;
  @Output() changed = new EventEmitter<GeneratedType<"FilterParameters">>();

  formGroup = this._formBuilder.group<{
    [key: string]: FormControl<boolean | null>;
  }>({});

  inverse = false;
  selected: string[] = [];

  /* veld: prefix */
  public VERTAALBARE_FACETTEN = {
    TAAK_STATUS: "taak.status.",
    TYPE: "type.",
    TOEGEKEND: "zoeken.filter.jaNee.",
    ZAAK_INDICATIES: "indicatie.",
    DOCUMENT_INDICATIES: "indicatie.",
    DOCUMENT_STATUS: "informatieobject.status.",
    ZAAK_VERTROUWELIJKHEIDAANDUIDING: "vertrouwelijkheidaanduiding.",
    ZAAK_ARCHIEF_NOMINATIE: "archiefNominatie.",
  };

  constructor(private _formBuilder: FormBuilder) {}

  ngOnInit(): void {
    this.inverse =
      this.filter?.inverse === true || String(this.filter?.inverse) === "true";
    this.selected = this.filter?.values ?? [];
    this.opties.forEach((value) => {
      this.formGroup.addControl(
        value.naam,
        new FormControl(!!this.selected.find((s) => s === value.naam)),
      );
    });
  }

  checkboxChange(): void {
    const checked = Object.keys(this.formGroup.controls).reduce<string[]>(
      (acc, key) => {
        if (this.formGroup.controls[key].value) acc.push(key);
        return acc;
      },
      [],
    );
    this.changed.emit({
      values: checked,
      inverse: this.inverse,
    });
  }

  isVertaalbaar(veld: string): boolean {
    return (
      this.VERTAALBARE_FACETTEN[
        veld as keyof typeof this.VERTAALBARE_FACETTEN
      ] !== undefined
    );
  }

  invert() {
    this.inverse = !this.inverse;
    if (Object.values(this.formGroup.value).includes(true)) {
      this.checkboxChange();
    }
  }
}
