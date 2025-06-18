/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
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
import { injectQuery } from "@tanstack/angular-query-experimental";
import { Observable, lastValueFrom, merge } from "rxjs";
import { map, startWith, switchMap } from "rxjs/operators";
import { UtilService } from "../../core/service/util.service";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZaakZoekObject } from "../../zoeken/model/zaken/zaak-zoek-object";
import { ZoekParameters } from "../../zoeken/model/zoek-parameters";
import { ZoekResultaat } from "../../zoeken/model/zoek-resultaat";
import { ZoekVeld } from "../../zoeken/model/zoek-veld";
import { ZoekenService } from "../../zoeken/zoeken.service";
import { KlantenService } from "../klanten.service";

@Component({
  selector: "zac-klant-zaken-tabel",
  templateUrl: "./klant-zaken-tabel.component.html",
  styleUrls: ["./klant-zaken-tabel.component.less"],
})
export class KlantZakenTabelComponent
  implements OnInit, AfterViewInit, OnChanges
{
  @Input() klantIdentificatie: string;
  @ViewChild(MatPaginator) paginator: MatPaginator;
  @ViewChild(MatSort) sort: MatSort;
  dataSource: MatTableDataSource<ZaakZoekObject> =
    new MatTableDataSource<ZaakZoekObject>();
  columns = [
    "identificatie",
    "betrokkene",
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
  sorteerVeld: GeneratedType<"SorteerVeld"> | null = null;
  filterChange = new EventEmitter<void>();
  zoekParameters = new ZoekParameters();
  actieveFilters = false;
  zoekResultaat = new ZoekResultaat<ZaakZoekObject>();
  init: boolean;
  inclusiefAfgerondeZaken = new FormControl(false);
  ZoekVeld = ZoekVeld;
  betrokkeneSelectControl = new FormControl<ZoekVeld>(null);
  private laatsteBetrokkenheid: string;

  distinctRoltypenQuery = injectQuery(() => ({
    queryKey: ["roltypen", "distinct"],
    queryFn: () => this.listDistinctRoltypen(),
  }));

  constructor(
    private utilService: UtilService,
    private zoekenService: ZoekenService,
    private klantenService: KlantenService,
  ) {}

  ngOnInit(): void {
    this.zoekParameters.type = "ZAAK";
  }

  private distinct<T>(values: T[]): T[] {
    return [...new Set(values)];
  }

  private listDistinctRoltypen() {
    const distinctRoltypen$ = this.klantenService
      .listRoltypen()
      .pipe(map((typen) => this.distinct(typen.map(({ naam }) => naam))));
    return lastValueFrom(distinctRoltypen$);
  }

  private loadZaken(): Observable<ZoekResultaat<ZaakZoekObject>> {
    if (this.laatsteBetrokkenheid) {
      delete this.zoekParameters.zoeken[this.laatsteBetrokkenheid];
    }
    if (this.betrokkeneSelectControl.value) {
      this.setZoekParameterBetrokkenheid(this.betrokkeneSelectControl.value);
    }
    this.actieveFilters = ZoekParameters.heeftActieveFilters(
      this.zoekParameters,
    ); // before default values
    if (!this.betrokkeneSelectControl.value) {
      this.setZoekParameterBetrokkenheid(ZoekVeld.ZAAK_BETROKKENEN);
    }
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

  private setZoekParameterBetrokkenheid(betrokkenheid: ZoekVeld) {
    this.zoekParameters.zoeken[(this.laatsteBetrokkenheid = betrokkenheid)] =
      this.klantIdentificatie;
  }

  ngAfterViewInit(): void {
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

  getBetrokkenheid(zaak: ZaakZoekObject): string[] {
    const rollen = [];
    Object.entries(zaak.betrokkenen).forEach((value: [string, string[]]) => {
      const rol = value[0];
      const ids = value[1];
      if (ids.includes(this.klantIdentificatie)) {
        rollen.push(rol);
      }
    });
    return rollen;
  }

  filtersChanged(): void {
    this.paginator.pageIndex = 0;
    this.filterChange.emit();
  }

  ngOnChanges(changes: SimpleChanges): void {
    this.klantIdentificatie = changes.klantIdentificatie.currentValue;
    if (this.init) {
      this.filtersChanged();
    }
  }

  clearFilters() {
    this.sort.sort({ id: null, start: "asc", disableClear: false });
    this.zoekParameters.zoeken = {};
    this.zoekParameters.filters = {};
    this.zoekParameters.datums = {};
    this.betrokkeneSelectControl.setValue(null);
    this.filtersChanged();
  }
}
