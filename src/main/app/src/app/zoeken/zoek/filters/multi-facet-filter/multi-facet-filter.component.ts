/*
 * SPDX-FileCopyrightText: 2021-2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NgFor, NgIf, LowerCasePipe } from "@angular/common";
import { Component, EventEmitter, Input, OnInit, Output } from "@angular/core";
import { FormBuilder, FormControl, ReactiveFormsModule } from "@angular/forms";
import { MatCardModule } from "@angular/material/card";
import { MatCheckboxModule } from "@angular/material/checkbox";
import { MatIconModule } from "@angular/material/icon";
import { TranslateModule } from "@ngx-translate/core";
import { ZacNarrowMatCheckboxDirective } from "../../../../shared/material/narrow-checkbox.directive";
import { ReadMoreComponent } from "../../../../shared/read-more/read-more.component";
import { GeneratedType } from "../../../../shared/utils/generated-types";

@Component({
  selector: "zac-multi-facet-filter",
  templateUrl: "./multi-facet-filter.component.html",
  styleUrls: ["./multi-facet-filter.component.less"],
  standalone: true,
  imports: [
    NgIf,
    NgFor,
    LowerCasePipe,
    ReactiveFormsModule,
    MatCardModule,
    MatCheckboxModule,
    MatIconModule,
    TranslateModule,
    ZacNarrowMatCheckboxDirective,
    ReadMoreComponent,
  ],
})
export class MultiFacetFilterComponent implements OnInit {
  @Input({ required: true }) filter!: GeneratedType<"FilterParameters">;
  @Input({ required: true }) opties!: GeneratedType<"FilterResultaat">[];
  @Input({ required: true }) label!: string;
  @Output() changed = new EventEmitter<GeneratedType<"FilterParameters">>();

  protected formGroup = this._formBuilder.group<{
    [key: string]: FormControl<boolean | null>;
  }>({});

  protected inverse = false;
  private selected: string[] = [];

  /* veld: prefix */
  protected VERTAALBARE_FACETTEN = {
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

  protected checkboxChange(): void {
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

  protected isVertaalbaar(veld: string): boolean {
    return (
      this.VERTAALBARE_FACETTEN[
        veld as keyof typeof this.VERTAALBARE_FACETTEN
      ] !== undefined
    );
  }

  protected invert() {
    this.inverse = !this.inverse;
    if (Object.values(this.formGroup.value).includes(true)) {
      this.checkboxChange();
    }
  }
}
