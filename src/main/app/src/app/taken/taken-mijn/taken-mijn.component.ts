/*
 * SPDX-FileCopyrightText: 2021 - 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  NgFor,
  NgIf,
  NgSwitch,
  NgSwitchCase,
  NgSwitchDefault,
  SlicePipe,
} from "@angular/common";
import {
  AfterViewInit,
  Component,
  OnDestroy,
  OnInit,
  ViewChild,
} from "@angular/core";

import { detailExpand } from "../../shared/animations/animations";

import { DragDropModule } from "@angular/cdk/drag-drop";
import { MatIconAnchor, MatIconButton } from "@angular/material/button";
import { MatIcon } from "@angular/material/icon";
import { MatPaginator } from "@angular/material/paginator";
import { MatSort, MatSortModule } from "@angular/material/sort";
import { MatTable, MatTableModule } from "@angular/material/table";
import { ActivatedRoute, RouterLink } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { UtilService } from "../../core/service/util.service";
import { ColumnPickerValue } from "../../shared/dynamic-table/column-picker/column-picker-value";
import { ColumnPickerComponent } from "../../shared/dynamic-table/column-picker/column-picker.component";
import { TextIcon } from "../../shared/edit/text-icon";
import { ExportButtonComponent } from "../../shared/export-button/export-button.component";
import { DagenPipe } from "../../shared/pipes/dagen.pipe";
import { DatumPipe } from "../../shared/pipes/datum.pipe";
import { EmptyPipe } from "../../shared/pipes/empty.pipe";
import { StaticTextComponent } from "../../shared/static-text/static-text.component";
import { DateRangeFilterComponent } from "../../shared/table-zoek-filters/date-range-filter/date-range-filter.component";
import { FacetFilterComponent } from "../../shared/table-zoek-filters/facet-filter/facet-filter.component";
import { TekstFilterComponent } from "../../shared/table-zoek-filters/tekst-filter/tekst-filter.component";
import { DateConditionals } from "../../shared/utils/date-conditionals";
import { TaakZoekObject } from "../../zoeken/model/taken/taak-zoek-object";
import { ZoekenService } from "../../zoeken/zoeken.service";

import { GebruikersvoorkeurenService } from "../../gebruikersvoorkeuren/gebruikersvoorkeuren.service";
import { ZoekopdrachtComponent } from "../../gebruikersvoorkeuren/zoekopdracht/zoekopdracht.component";
import { WerklijstComponent } from "../../shared/dynamic-table/datasource/werklijst-component";
import { ZoekenColumn } from "../../shared/dynamic-table/model/zoeken-column";
import { GeneratedType } from "../../shared/utils/generated-types";
import { TakenMijnDatasource } from "./taken-mijn-datasource";

@Component({
  templateUrl: "./taken-mijn.component.html",
  styleUrls: ["./taken-mijn.component.less"],
  animations: [detailExpand],
  standalone: true,
  imports: [
    DragDropModule,
    MatTableModule,
    MatSortModule,
    MatPaginator,
    MatIcon,
    MatIconButton,
    MatIconAnchor,
    RouterLink,
    TranslateModule,
    NgIf,
    NgFor,
    NgSwitch,
    NgSwitchCase,
    NgSwitchDefault,
    SlicePipe,
    EmptyPipe,
    DatumPipe,
    DagenPipe,
    FacetFilterComponent,
    TekstFilterComponent,
    DateRangeFilterComponent,
    ZoekopdrachtComponent,
    ColumnPickerComponent,
    ExportButtonComponent,
    StaticTextComponent,
  ],
})
export class TakenMijnComponent
  extends WerklijstComponent
  implements AfterViewInit, OnInit, OnDestroy
{
  dataSource: TakenMijnDatasource;
  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;
  @ViewChild(MatTable) table!: MatTable<TaakZoekObject>;
  expandedRow: TaakZoekObject | null = null;
  readonly zoekenColumn = ZoekenColumn;

  fataledatumIcon: TextIcon = new TextIcon(
    DateConditionals.provideFormControlValue(DateConditionals.isExceeded),
    "report_problem",
    "errorVerlopen_icon",
    "msg.datum.overschreden",
    "error",
  );

  constructor(
    public route: ActivatedRoute,
    public utilService: UtilService,
    private zoekenService: ZoekenService,
    public gebruikersvoorkeurenService: GebruikersvoorkeurenService,
  ) {
    super();
    this.dataSource = new TakenMijnDatasource(
      this.zoekenService,
      this.utilService,
    );
  }

  ngOnInit(): void {
    super.ngOnInit();
    this.utilService.setTitle("title.taken.mijn");
    this.dataSource.initColumns(this.defaultColumns());
  }

  ngAfterViewInit(): void {
    this.dataSource.setViewChilds(this.paginator, this.sort);
    this.table.dataSource = this.dataSource;
  }

  protected isAfterDate(datum: Date | string | null): boolean {
    return DateConditionals.isExceeded(datum);
  }

  defaultColumns(): Map<ZoekenColumn, ColumnPickerValue> {
    return new Map([
      [ZoekenColumn.NAAM, ColumnPickerValue.VISIBLE],
      [ZoekenColumn.ZAAK_IDENTIFICATIE, ColumnPickerValue.VISIBLE],
      [ZoekenColumn.ZAAK_OMSCHRIJVING, ColumnPickerValue.VISIBLE],
      [ZoekenColumn.ZAAK_TOELICHTING, ColumnPickerValue.HIDDEN],
      [ZoekenColumn.ZAAKTYPE_OMSCHRIJVING, ColumnPickerValue.VISIBLE],
      [ZoekenColumn.CREATIEDATUM, ColumnPickerValue.VISIBLE],
      [ZoekenColumn.FATALEDATUM, ColumnPickerValue.VISIBLE],
      [ZoekenColumn.DAGEN_TOT_FATALEDATUM, ColumnPickerValue.VISIBLE],
      [ZoekenColumn.GROEP, ColumnPickerValue.VISIBLE],
      [ZoekenColumn.TOELICHTING, ColumnPickerValue.HIDDEN],
      [ZoekenColumn.URL, ColumnPickerValue.STICKY],
    ]);
  }

  getWerklijst(): GeneratedType<"Werklijst"> {
    return "WERKVOORRAAD_TAKEN";
  }

  resetColumns(): void {
    this.dataSource.resetColumns();
  }

  filtersChange(): void {
    this.dataSource.filtersChanged();
  }

  ngOnDestroy(): void {
    // Make sure when returning to this comnponent, the very first page is loaded
    this.dataSource.zoekopdrachtResetToFirstPage();
  }
}
