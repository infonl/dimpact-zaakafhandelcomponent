/*
 * SPDX-FileCopyrightText: 2023 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NgIf } from "@angular/common";
import {
  AfterViewInit,
  Component,
  EventEmitter,
  Input,
  OnChanges,
  OnInit,
  ViewChild,
} from "@angular/core";
import { FormControl, ReactiveFormsModule } from "@angular/forms";
import { MatButtonModule } from "@angular/material/button";
import { MatCardModule } from "@angular/material/card";
import { MatIconModule } from "@angular/material/icon";
import { MatPaginator, MatPaginatorModule } from "@angular/material/paginator";
import { MatSlideToggleModule } from "@angular/material/slide-toggle";
import { MatSort, MatSortModule } from "@angular/material/sort";
import { MatTableDataSource, MatTableModule } from "@angular/material/table";
import { RouterModule } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { Observable, merge } from "rxjs";
import { map, startWith, switchMap } from "rxjs/operators";
import { UtilService } from "../../core/service/util.service";
import { DatumPipe } from "../../shared/pipes/datum.pipe";
import { EmptyPipe } from "../../shared/pipes/empty.pipe";
import { DateRangeFilterComponent } from "../../shared/table-zoek-filters/date-range-filter/date-range-filter.component";
import { FacetFilterComponent } from "../../shared/table-zoek-filters/facet-filter/facet-filter.component";
import { TekstFilterComponent } from "../../shared/table-zoek-filters/tekst-filter/tekst-filter.component";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZaakZoekObject } from "../../zoeken/model/zaken/zaak-zoek-object";
import { getDefaultZoekParameters } from "../../zoeken/model/zoek-parameters";
import { ZoekResultaat } from "../../zoeken/model/zoek-resultaat";
import { ZoekenService } from "../../zoeken/zoeken.service";

@Component({
  selector: "zac-bag-zaken-tabel",
  templateUrl: "./bag-zaken-tabel.component.html",
  styleUrls: ["./bag-zaken-tabel.component.less"],
  standalone: true,
  imports: [
    NgIf,
    ReactiveFormsModule,
    MatCardModule,
    MatTableModule,
    MatSortModule,
    MatPaginatorModule,
    MatSlideToggleModule,
    MatIconModule,
    MatButtonModule,
    RouterModule,
    TranslateModule,
    TekstFilterComponent,
    FacetFilterComponent,
    DateRangeFilterComponent,
    DatumPipe,
    EmptyPipe,
  ],
})
export class BagZakenTabelComponent
  implements OnInit, AfterViewInit, OnChanges
{
  @Input({ required: true }) BagObjectIdentificatie!: string;
  @ViewChild(MatPaginator) private paginator!: MatPaginator;
  @ViewChild(MatSort) private sort!: MatSort;
  protected dataSource = new MatTableDataSource<ZaakZoekObject>();
  protected columns = [
    "identificatie",
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
  filterChange = new EventEmitter<void>();
  protected zoekParameters = getDefaultZoekParameters();
  protected zoekResultaat = new ZoekResultaat<ZaakZoekObject>();
  private init = false;
  protected inclusiefAfgerondeZaken = new FormControl(false);

  constructor(
    private readonly utilService: UtilService,
    private readonly zoekenService: ZoekenService,
  ) {}

  ngOnInit() {
    this.zoekParameters.type = "ZAAK";
    this.zoekParameters.zoeken ??= {};
    this.zoekParameters.zoeken.ZAAK_BAGOBJECTEN = this.BagObjectIdentificatie;
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

  protected filtersChanged() {
    this.paginator.pageIndex = 0;
    this.filterChange.emit();
  }

  ngOnChanges() {
    if (this.init) {
      this.filtersChanged();
    }
  }
}
