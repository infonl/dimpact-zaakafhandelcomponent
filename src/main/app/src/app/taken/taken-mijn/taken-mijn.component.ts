/*
 * SPDX-FileCopyrightText: 2021 - 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  AfterViewInit,
  Component,
  OnDestroy,
  OnInit,
  ViewChild,
} from "@angular/core";

import { detailExpand } from "../../shared/animations/animations";

import { MatPaginator } from "@angular/material/paginator";
import { MatSort } from "@angular/material/sort";
import { MatTable } from "@angular/material/table";
import { ActivatedRoute } from "@angular/router";
import { UtilService } from "../../core/service/util.service";
import { IdentityService } from "../../identity/identity.service";
import { ColumnPickerValue } from "../../shared/dynamic-table/column-picker/column-picker-value";
import { TextIcon } from "../../shared/edit/text-icon";
import { DateConditionals } from "../../shared/utils/date-conditionals";
import { TaakZoekObject } from "../../zoeken/model/taken/taak-zoek-object";
import { ZoekenService } from "../../zoeken/zoeken.service";
import { TakenService } from "../taken.service";

import { GebruikersvoorkeurenService } from "../../gebruikersvoorkeuren/gebruikersvoorkeuren.service";
import { WerklijstComponent } from "../../shared/dynamic-table/datasource/werklijst-component";
import { ZoekenColumn } from "../../shared/dynamic-table/model/zoeken-column";
import { GeneratedType } from "../../shared/utils/generated-types";
import { TakenMijnDatasource } from "./taken-mijn-datasource";

@Component({
  templateUrl: "./taken-mijn.component.html",
  styleUrls: ["./taken-mijn.component.less"],
  animations: [detailExpand],
})
export class TakenMijnComponent
  extends WerklijstComponent
  implements AfterViewInit, OnInit, OnDestroy
{
  dataSource: TakenMijnDatasource;
  @ViewChild(MatPaginator) paginator: MatPaginator;
  @ViewChild(MatSort) sort: MatSort;
  @ViewChild(MatTable) table: MatTable<TaakZoekObject>;
  expandedRow: TaakZoekObject | null;
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
    private takenService: TakenService,
    public utilService: UtilService,
    private identityService: IdentityService,
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

  isAfterDate(datum): boolean {
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
