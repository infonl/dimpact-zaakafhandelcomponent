/*
 * SPDX-FileCopyrightText: 2023 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  AfterViewInit,
  Component,
  EventEmitter,
  Input,
  OnChanges,
  OnInit,
  SimpleChanges,
  ViewChild,
} from "@angular/core";
import { FormControl } from "@angular/forms";
import { MatPaginator } from "@angular/material/paginator";
import { MatSort } from "@angular/material/sort";
import { MatTableDataSource } from "@angular/material/table";
import { Observable, merge } from "rxjs";
import { map, startWith, switchMap } from "rxjs/operators";
import { UtilService } from "../../core/service/util.service";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZaakZoekObject } from "../../zoeken/model/zaken/zaak-zoek-object";
import { DEFAULT_ZOEK_PARAMETERS } from "../../zoeken/model/zoek-parameters";
import { ZoekResultaat } from "../../zoeken/model/zoek-resultaat";
import { ZoekVeld } from "../../zoeken/model/zoek-veld";
import { ZoekenService } from "../../zoeken/zoeken.service";

@Component({
  selector: "zac-bag-zaken-tabel",
  templateUrl: "./bag-zaken-tabel.component.html",
  styleUrls: ["./bag-zaken-tabel.component.less"],
})
export class BagZakenTabelComponent
  implements OnInit, AfterViewInit, OnChanges
{
  @Input({ required: true }) BagObjectIdentificatie!: string;
  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;
  dataSource = new MatTableDataSource<ZaakZoekObject>();
  columns = [
    "identificatie",
    "status",
    "groep",
    "behandelaar",
    "startdatum",
    "zaaktype",
    "omschrijving",
    "url",
  ] as const;
  filterColumns = this.columns.map((n) => n + "_filter");
  isLoadingResults = true;
  filterChange = new EventEmitter<void>();
  zoekParameters = DEFAULT_ZOEK_PARAMETERS;
  zoekResultaat = new ZoekResultaat<ZaakZoekObject>();
  init = false;
  inclusiefAfgerondeZaken = new FormControl(false);
  ZoekVeld = ZoekVeld;

  constructor(
    private utilService: UtilService,
    private zoekenService: ZoekenService,
  ) {}

  ngOnInit(): void {
    this.zoekParameters.type = "ZAAK";
    if (this.zoekParameters.zoeken) {
      this.zoekParameters.zoeken.ZAAK_BAGOBJECTEN = this.BagObjectIdentificatie;
    }
  }

  private loadZaken() {
    if (!this.zoekParameters.zoeken) {
      this.zoekParameters.zoeken = {};
    }
    this.zoekParameters.zoeken.ZAAK_BAGOBJECTEN = this.BagObjectIdentificatie;
    this.zoekParameters.page = this.paginator.pageIndex;
    this.zoekParameters.sorteerRichting = this.sort.direction;
    this.zoekParameters.sorteerVeld = this.sort
      .active as GeneratedType<"SorteerVeld">;
    this.zoekParameters.rows = this.paginator.pageSize;
    this.zoekParameters.alleenOpenstaandeZaken =
      !this.inclusiefAfgerondeZaken.value;
    return this.zoekenService.list(this.zoekParameters) as Observable<
      ZoekResultaat<ZaakZoekObject>
    >;
  }

  ngAfterViewInit() {
    this.init = true;
    this.filtersChanged();
    this.sort.sortChange.subscribe(() => (this.paginator.pageIndex = 0));
    merge(this.sort.sortChange, this.paginator.page, this.filterChange)
      .pipe(
        startWith({}),
        switchMap(() => {
          this.isLoadingResults = true;
          this.utilService.setLoading(true);
          return this.loadZaken();
        }),
        map((zoekResultaat) => {
          this.isLoadingResults = false;
          this.utilService.setLoading(false);
          return zoekResultaat;
        }),
      )
      .subscribe((zoekResultaat) => {
        this.zoekResultaat = zoekResultaat;
        this.paginator.length = zoekResultaat.totaal;
        this.dataSource.data = zoekResultaat.resultaten;
      });
  }

  filtersChanged() {
    this.paginator.pageIndex = 0;
    this.filterChange.emit();
  }

  ngOnChanges(changes: SimpleChanges) {
    this.BagObjectIdentificatie = changes.BagObjectIdentificatie.currentValue;
    if (this.init) {
      this.filtersChanged();
    }
  }
}
