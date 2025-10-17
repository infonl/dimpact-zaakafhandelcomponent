/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { CdkDragDrop, moveItemInArray } from "@angular/cdk/drag-drop";
import { Component, OnInit, ViewChild } from "@angular/core";
import { Validators } from "@angular/forms";
import { MatSidenav, MatSidenavContainer } from "@angular/material/sidenav";
import { MatTableDataSource } from "@angular/material/table";
import { ActivatedRoute } from "@angular/router";
import { of } from "rxjs";
import { catchError } from "rxjs/operators";
import { ConfiguratieService } from "../../configuratie/configuratie.service";
import { UtilService } from "../../core/service/util.service";
import { FoutAfhandelingService } from "../../fout-afhandeling/fout-afhandeling.service";
import { InputFormField } from "../../shared/material-form-builder/form-components/input/input-form-field";
import { InputFormFieldBuilder } from "../../shared/material-form-builder/form-components/input/input-form-field-builder";
import { GeneratedType } from "../../shared/utils/generated-types";
import { AdminComponent } from "../admin/admin.component";
import { ReferentieTabelService } from "../referentie-tabel.service";

@Component({
  templateUrl: "./referentie-tabel.component.html",
  styleUrls: ["./referentie-tabel.component.less"],
})
export class ReferentieTabelComponent extends AdminComponent implements OnInit {
  @ViewChild("sideNavContainer") sideNavContainer!: MatSidenavContainer;
  @ViewChild("menuSidenav") menuSidenav!: MatSidenav;

  tabel: GeneratedType<"RestReferenceTable"> = {
    code: "VUL SVP EEN UNIEKE TABEL CODE IN",
    naam: "Nieuwe referentietabel",
    systeem: false,
    waarden: [],
    aantalWaarden: 0,
  };

  codeFormField: InputFormField;
  naamFormField: InputFormField;

  isLoadingResults = false;
  columns = ["naam", "id"] as const;
  dataSource = new MatTableDataSource<
    GeneratedType<"RestReferenceTableValue">
  >();

  waardeFormField: InputFormField[] = [];

  constructor(
    public readonly utilService: UtilService,
    public readonly configuratieService: ConfiguratieService,
    private readonly service: ReferentieTabelService,
    private readonly route: ActivatedRoute,
    private readonly foutAfhandelingService: FoutAfhandelingService,
  ) {
    super(utilService, configuratieService);
  }

  ngOnInit() {
    this.route.data.subscribe((data) => {
      this.init(data.tabel);
    });
  }

  init(tabel?: GeneratedType<"RestReferenceTable">) {
    this.tabel = tabel ?? this.tabel;
    this.setupMenu("title.referentietabel", { tabel: this.tabel.code });
    this.createForm();
    this.laadTabelWaarden();
  }

  createForm() {
    this.codeFormField = new InputFormFieldBuilder(this.tabel.code)
      .id("code")
      .label("tabel")
      .validators(Validators.required)
      .build();
    this.naamFormField = new InputFormFieldBuilder(this.tabel.naam)
      .id("naam")
      .label("naam")
      .validators(Validators.required)
      .build();
  }

  editTabel(
    event: Record<string, unknown>,
    field: keyof typeof this.tabel,
  ): void {
    this.tabel[field] = event[field] as never;
    this.persistTabel();
  }

  laadTabelWaarden() {
    this.isLoadingResults = true;
    this.tabel.waarden.forEach((waarde) => {
      this.waardeFormField[waarde.id!] = new InputFormFieldBuilder(waarde.naam)
        .id("waarde_" + waarde.id)
        .label("waarde")
        .validators(Validators.required)
        .build();
    });
    this.dataSource.data = this.tabel.waarden;
    this.isLoadingResults = false;
  }

  nieuweTabelWaarde() {
    this.tabel.waarden.push({
      naam: this.getUniqueNaam(1),
    });
    this.persistTabel();
  }

  editTabelWaarde(
    event: Record<string, unknown>,
    row: GeneratedType<"RestReferenceTableValue">,
  ) {
    const naam = event["waarde_" + row.id];
    for (const waarde of this.tabel.waarden) {
      if (waarde.naam === naam) {
        this.foutAfhandelingService.openFoutDialog(
          'Deze referentietabel bevat al een "' + naam + '" waarde.',
        );
        return;
      }
    }
    this.tabel.waarden[this.getTabelWaardeIndex(row)].naam = String(naam);
    this.persistTabel();
  }

  moveTabelWaarde(
    event: CdkDragDrop<GeneratedType<"RestReferenceTableValue">[]>,
  ) {
    const sameRow = event.previousIndex === event.currentIndex;
    if (!sameRow) {
      moveItemInArray(
        event.container.data,
        event.previousIndex,
        event.currentIndex,
      );
      this.persistTabel();
    }
  }

  verwijderTabelWaarde(row: GeneratedType<"RestReferenceTableValue">) {
    this.tabel.waarden.splice(this.getTabelWaardeIndex(row), 1);
    this.persistTabel();
  }

  private getTabelWaardeIndex(row: GeneratedType<"RestReferenceTableValue">) {
    return this.tabel.waarden.findIndex((waarde) => waarde.id === row.id);
  }

  private getUniqueNaam(i: number) {
    let naam: string = "Nieuwe waarde" + (1 < i ? " " + i : "");
    this.tabel.waarden.forEach((waarde) => {
      if (waarde.naam === naam) {
        naam = this.getUniqueNaam(i + 1);
        return;
      }
    });
    return naam;
  }

  private persistTabel() {
    const persistReferentieTabel =
      this.tabel.id != null
        ? this.service.updateReferentieTabel(this.tabel.id, this.tabel)
        : this.service.createReferentieTabel(this.tabel);
    persistReferentieTabel
      .pipe(catchError(() => of(this.tabel)))
      .subscribe((persistedTabel) => {
        this.init(persistedTabel);
      });
  }
}
