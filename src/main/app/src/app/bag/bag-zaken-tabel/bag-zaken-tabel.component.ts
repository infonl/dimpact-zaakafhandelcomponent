/*
 * SPDX-FileCopyrightText: 2023 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  AfterViewInit,
  Component,
  EventEmitter,
  Input,
  OnChanges,
  OnDestroy,
  OnInit,
  SimpleChanges,
  ViewChild,
} from "@angular/core";
import { MatPaginator } from "@angular/material/paginator";
import { MatSort } from "@angular/material/sort";
import { MatTableDataSource } from "@angular/material/table";
import { Observable, Subject, merge } from "rxjs";
import { map, startWith, switchMap, takeUntil } from "rxjs/operators";
import { UtilService } from "../../core/service/util.service";
import { SorteerVeld } from "../../zoeken/model/sorteer-veld";
import { ZaakZoekObject } from "../../zoeken/model/zaken/zaak-zoek-object";
import { ZoekObjectType } from "../../zoeken/model/zoek-object-type";
import { ZoekParameters } from "../../zoeken/model/zoek-parameters";
import { ZoekResultaat } from "../../zoeken/model/zoek-resultaat";
import { ZoekVeld } from "../../zoeken/model/zoek-veld";
import { ZoekenService } from "../../zoeken/zoeken.service";

@Component({
  selector: "zac-bag-zaken-tabel",
  templateUrl: "./bag-zaken-tabel.component.html",
  styleUrls: ["./bag-zaken-tabel.component.less"],
})
export class BagZakenTabelComponent
  implements OnInit, AfterViewInit, OnChanges, OnDestroy
{
  @Input() BagObjectIdentificatie: string;
  @ViewChild(MatPaginator) paginator: MatPaginator;
  @ViewChild(MatSort) sort: MatSort;
  dataSource: MatTableDataSource<ZaakZoekObject> =
    new MatTableDataSource<ZaakZoekObject>();
  columns: string[] = [
    "identificatie",
    "status",
    "groep",
    "behandelaar",
    "startdatum",
    "zaaktype",
    "omschrijving",
    "url",
  ];
  filterColumns: string[] = this.columns.map((n) => n + "_filter");
  isLoadingResults = true;
  sorteerVeld = SorteerVeld;
  filterChange: EventEmitter<void> = new EventEmitter<void>();
  zoekParameters = new ZoekParameters();
  zoekResultaat: ZoekResultaat<ZaakZoekObject> =
    new ZoekResultaat<ZaakZoekObject>();
  init: boolean;
  inclusiefAfgerondeZaken = false;
  ZoekVeld = ZoekVeld;
  destroy$ = new Subject<void>();

  constructor(
    private utilService: UtilService,
    private zoekenService: ZoekenService,
  ) {}
  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  ngOnInit(): void {
    this.zoekParameters.type = ZoekObjectType.ZAAK;
    this.zoekParameters.zoeken.ZAAK_BAGOBJECTEN = this.BagObjectIdentificatie;
  }

  private loadZaken(): Observable<ZoekResultaat<ZaakZoekObject>> {
    if (!this.zoekParameters.zoeken) {
      this.zoekParameters.zoeken = {};
    }
    this.zoekParameters.zoeken.ZAAK_BAGOBJECTEN = this.BagObjectIdentificatie;
    this.zoekParameters.page = this.paginator.pageIndex;
    this.zoekParameters.sorteerRichting = this.sort.direction;
    this.zoekParameters.sorteerVeld = SorteerVeld[this.sort.active];
    this.zoekParameters.rows = this.paginator.pageSize;
    this.zoekParameters.alleenOpenstaandeZaken = !this.inclusiefAfgerondeZaken;
    return this.zoekenService.list(this.zoekParameters) as Observable<
      ZoekResultaat<ZaakZoekObject>
    >;
  }

  ngAfterViewInit(): void {
    this.init = true;
    this.filtersChanged();
    this.sort.sortChange
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => (this.paginator.pageIndex = 0));
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
        takeUntil(this.destroy$),
      )
      .subscribe((zoekResultaat) => {
        this.zoekResultaat = zoekResultaat;
        this.paginator.length = zoekResultaat.totaal;
        this.dataSource.data = zoekResultaat.resultaten;
      });
  }

  filtersChanged(): void {
    this.paginator.pageIndex = 0;
    this.filterChange.emit();
  }

  ngOnChanges(changes: SimpleChanges): void {
    this.BagObjectIdentificatie = changes.BagObjectIdentificatie.currentValue;
    if (this.init) {
      this.filtersChanged();
    }
  }
}
