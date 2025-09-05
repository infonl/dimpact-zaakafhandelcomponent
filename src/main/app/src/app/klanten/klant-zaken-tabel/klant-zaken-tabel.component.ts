/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  AfterViewInit,
  Component,
  EventEmitter,
  input,
  ViewChild,
} from "@angular/core";
import { FormControl } from "@angular/forms";
import { MatPaginator } from "@angular/material/paginator";
import { MatSort } from "@angular/material/sort";
import { MatTableDataSource } from "@angular/material/table";
import { injectQuery } from "@tanstack/angular-query-experimental";
import { merge, Observable } from "rxjs";
import { map, startWith, switchMap } from "rxjs/operators";
import { UtilService } from "../../core/service/util.service";
import { ZoekFilters } from "../../gebruikersvoorkeuren/zoekopdracht/zoekfilters.model";
import { GeneratedType } from "../../shared/utils/generated-types";
import { BetrokkeneIdentificatie } from "../../zaken/model/betrokkeneIdentificatie";
import { ZaakZoekObject } from "../../zoeken/model/zaken/zaak-zoek-object";
import {
  DEFAULT_ZOEK_PARAMETERS,
  heeftActieveZoekFilters,
} from "../../zoeken/model/zoek-parameters";
import { ZoekResultaat } from "../../zoeken/model/zoek-resultaat";
import { ZoekVeld } from "../../zoeken/model/zoek-veld";
import { ZoekenService } from "../../zoeken/zoeken.service";
import { KlantenService } from "../klanten.service";

@Component({
  selector: "zac-klant-zaken-tabel",
  templateUrl: "./klant-zaken-tabel.component.html",
  styleUrls: ["./klant-zaken-tabel.component.less"],
})
export class KlantZakenTabelComponent implements AfterViewInit {
  protected readonly klant =
    input.required<GeneratedType<"RestBedrijf" | "RestPersoon">>();

  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;

  protected dataSource = new MatTableDataSource<ZaakZoekObject>();
  protected columns = [
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
  protected filterColumns = this.columns.map((n) => n + "_filter");
  protected isLoadingResults = true;
  protected filterChange = new EventEmitter<void>();
  protected zoekParameters: GeneratedType<"RestZoekParameters"> = {
    ...DEFAULT_ZOEK_PARAMETERS,
    type: "ZAAK",
  };
  protected actieveFilters = false;
  protected zoekResultaat = new ZoekResultaat<ZaakZoekObject>();
  protected init: boolean = false;
  protected inclusiefAfgerondeZaken = new FormControl(false);
  protected betrokkeneSelectControl = new FormControl<ZoekVeld | null>(null);
  private laatsteBetrokkenheid?: string | null = null;

  protected distinctRoltypenQuery = injectQuery(() => ({
    queryKey: ["roltypen", "distinct"],
    queryFn: () =>
      this.klantenService
        .listRoltypen()
        .pipe(map((typen) => this.distinct(typen.map(({ naam }) => naam)))),
  }));

  constructor(
    private readonly utilService: UtilService,
    private readonly zoekenService: ZoekenService,
    private readonly klantenService: KlantenService,
  ) {}

  private distinct<T>(values: T[]): T[] {
    return [...new Set(values)];
  }

  private loadZaken(): Observable<ZoekResultaat<ZaakZoekObject>> {
    if (this.laatsteBetrokkenheid) {
      delete this.zoekParameters.zoeken?.[this.laatsteBetrokkenheid];
    }
    if (this.betrokkeneSelectControl.value) {
      this.setZoekParameterBetrokkenheid(this.betrokkeneSelectControl.value);
    }
    this.actieveFilters =
      this.zoekParameters &&
      heeftActieveZoekFilters(this.zoekParameters as unknown as ZoekFilters); // before default values
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
    if (!this.zoekParameters.zoeken) this.zoekParameters.zoeken = {};
    const betrokkene = new BetrokkeneIdentificatie(this.klant());
    this.zoekParameters.zoeken[(this.laatsteBetrokkenheid = betrokkenheid)] =
      betrokkene.bsnNummer ?? betrokkene.kvkNummer ?? "";
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

  private getBetrokkenheid(zaak: ZaakZoekObject) {
    const betrokkene = new BetrokkeneIdentificatie(this.klant());
    return Array.from(zaak.betrokkenen).reduce((acc, [rol, ids]) => {
      if (betrokkene.bsnNummer && ids.includes(betrokkene.bsnNummer)) {
        acc.push(rol);
      }
      if (betrokkene.kvkNummer && ids.includes(betrokkene.kvkNummer)) {
        acc.push(rol);
      }
      return acc;
    }, [] as string[]);
  }

  protected filtersChanged() {
    this.paginator.pageIndex = 0;
    this.filterChange.emit();
  }

  public clearFilters() {
    this.sort.sort({ id: "", start: "asc", disableClear: false });
    this.zoekParameters.zoeken = {};
    this.zoekParameters.filters = {};
    this.zoekParameters.datums = {};
    this.betrokkeneSelectControl.setValue(null);
    this.filtersChanged();
  }
}
