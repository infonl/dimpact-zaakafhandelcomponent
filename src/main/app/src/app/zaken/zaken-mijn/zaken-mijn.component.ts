/*
 * SPDX-FileCopyrightText: 2021 - 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { CdkDrag, CdkDropList } from "@angular/cdk/drag-drop";
import {
  NgFor,
  NgIf,
  NgSwitch,
  NgSwitchCase,
  NgSwitchDefault,
  SlicePipe,
} from "@angular/common";
import { AfterViewInit, Component, OnDestroy, ViewChild } from "@angular/core";
import { MatIconAnchor, MatIconButton } from "@angular/material/button";
import { MatIcon } from "@angular/material/icon";
import { MatPaginator } from "@angular/material/paginator";
import { MatSort, MatSortHeader } from "@angular/material/sort";
import { MatTable, MatTableModule } from "@angular/material/table";
import { ActivatedRoute, RouterLink } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import moment from "moment";
import { DateConditionals } from "src/app/shared/utils/date-conditionals";
import { UtilService } from "../../core/service/util.service";
import { GebruikersvoorkeurenService } from "../../gebruikersvoorkeuren/gebruikersvoorkeuren.service";
import { ZoekopdrachtComponent } from "../../gebruikersvoorkeuren/zoekopdracht/zoekopdracht.component";
import { ColumnPickerValue } from "../../shared/dynamic-table/column-picker/column-picker-value";
import { ColumnPickerComponent } from "../../shared/dynamic-table/column-picker/column-picker.component";
import { WerklijstComponent } from "../../shared/dynamic-table/datasource/werklijst-component";
import { ZoekenColumn } from "../../shared/dynamic-table/model/zoeken-column";
import { TextIcon } from "../../shared/edit/text-icon";
import { ExportButtonComponent } from "../../shared/export-button/export-button.component";
import { IndicatiesLayout } from "../../shared/indicaties/indicaties.component";
import { ZaakIndicatiesComponent } from "../../shared/indicaties/zaak-indicaties/zaak-indicaties.component";
import { DagenPipe } from "../../shared/pipes/dagen.pipe";
import { DatumPipe } from "../../shared/pipes/datum.pipe";
import { EmptyPipe } from "../../shared/pipes/empty.pipe";
import { VertrouwelijkaanduidingToTranslationKeyPipe } from "../../shared/pipes/vertrouwelijkaanduiding-to-translation-key.pipe";
import { StaticTextComponent } from "../../shared/static-text/static-text.component";
import { DateRangeFilterComponent } from "../../shared/table-zoek-filters/date-range-filter/date-range-filter.component";
import { FacetFilterComponent } from "../../shared/table-zoek-filters/facet-filter/facet-filter.component";
import { TekstFilterComponent } from "../../shared/table-zoek-filters/tekst-filter/tekst-filter.component";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZaakZoekObject } from "../../zoeken/model/zaken/zaak-zoek-object";
import { ZoekenService } from "../../zoeken/zoeken.service";
import { ZakenMijnDatasource } from "./zaken-mijn-datasource";

import { detailExpand } from "../../shared/animations/animations";

@Component({
  templateUrl: "./zaken-mijn.component.html",
  styleUrls: ["./zaken-mijn.component.less"],
  animations: [detailExpand],
  standalone: true,
  imports: [
    CdkDrag,
    CdkDropList,
    NgFor,
    NgIf,
    NgSwitch,
    NgSwitchCase,
    NgSwitchDefault,
    SlicePipe,
    MatIconButton,
    MatIconAnchor,
    MatIcon,
    MatPaginator,
    MatSort,
    MatSortHeader,
    MatTableModule,
    RouterLink,
    TranslateModule,
    ZoekopdrachtComponent,
    ColumnPickerComponent,
    ExportButtonComponent,
    ZaakIndicatiesComponent,
    DagenPipe,
    DatumPipe,
    EmptyPipe,
    VertrouwelijkaanduidingToTranslationKeyPipe,
    StaticTextComponent,
    DateRangeFilterComponent,
    FacetFilterComponent,
    TekstFilterComponent,
  ],
})
export class ZakenMijnComponent
  extends WerklijstComponent
  implements AfterViewInit, OnDestroy
{
  readonly indicatiesLayout = IndicatiesLayout;
  dataSource: ZakenMijnDatasource;
  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;
  @ViewChild(MatTable) table!: MatTable<ZaakZoekObject>;
  expandedRow: ZaakZoekObject | null = null;
  readonly zoekenColumn = ZoekenColumn;

  einddatumGeplandIcon = new TextIcon(
    DateConditionals.provideFormControlValue(DateConditionals.isExceeded),
    "report_problem",
    "warningVerlopen_icon",
    "msg.datum.overschreden",
    "warning",
  );
  uiterlijkeEinddatumAfdoeningIcon = new TextIcon(
    DateConditionals.provideFormControlValue(DateConditionals.isExceeded),
    "report_problem",
    "errorVerlopen_icon",
    "msg.datum.overschreden",
    "error",
  );

  constructor(
    public gebruikersvoorkeurenService: GebruikersvoorkeurenService,
    public route: ActivatedRoute,
    private zoekenService: ZoekenService,
    public utilService: UtilService,
  ) {
    super();
    this.dataSource = new ZakenMijnDatasource(
      this.zoekenService,
      this.utilService,
    );
    this.utilService.setTitle("title.zaken.mijn");
    this.dataSource.initColumns(this.defaultColumns());
  }

  defaultColumns(): Map<ZoekenColumn, ColumnPickerValue> {
    return new Map([
      [ZoekenColumn.ZAAK_DOT_IDENTIFICATIE, ColumnPickerValue.VISIBLE],
      [ZoekenColumn.STATUS, ColumnPickerValue.VISIBLE],
      [ZoekenColumn.ZAAKTYPE, ColumnPickerValue.VISIBLE],
      [ZoekenColumn.OMSCHRIJVING, ColumnPickerValue.VISIBLE],
      [ZoekenColumn.GROEP, ColumnPickerValue.HIDDEN],
      [ZoekenColumn.COMMUNICATIEKANAAL, ColumnPickerValue.HIDDEN],
      [ZoekenColumn.VERTROUWELIJKHEIDAANDUIDING, ColumnPickerValue.HIDDEN],
      [ZoekenColumn.STARTDATUM, ColumnPickerValue.VISIBLE],
      [ZoekenColumn.REGISTRATIEDATUM, ColumnPickerValue.HIDDEN],
      [ZoekenColumn.OPENSTAANDE_TAKEN, ColumnPickerValue.VISIBLE],
      [ZoekenColumn.EINDDATUM, ColumnPickerValue.HIDDEN],
      [ZoekenColumn.EINDDATUM_GEPLAND, ColumnPickerValue.HIDDEN],
      [ZoekenColumn.DAGEN_TOT_STREEFDATUM, ColumnPickerValue.VISIBLE],
      [ZoekenColumn.UITERLIJKE_EINDDATUM_AFDOENING, ColumnPickerValue.HIDDEN],
      [ZoekenColumn.DAGEN_TOT_FATALEDATUM, ColumnPickerValue.VISIBLE],
      [ZoekenColumn.INDICATIES, ColumnPickerValue.VISIBLE],
      [ZoekenColumn.TOELICHTING, ColumnPickerValue.HIDDEN],
      [ZoekenColumn.URL, ColumnPickerValue.STICKY],
    ]);
  }

  ngAfterViewInit() {
    this.dataSource.setViewChilds(this.paginator, this.sort);
    this.table.dataSource = this.dataSource;
  }

  isAfterDate(datum: Date | moment.Moment | string) {
    return DateConditionals.isExceeded(datum);
  }

  resetColumns() {
    this.dataSource.resetColumns();
  }

  filtersChange() {
    this.dataSource.filtersChanged();
  }

  getWerklijst(): GeneratedType<"Werklijst"> {
    return "MIJN_ZAKEN";
  }

  ngOnDestroy() {
    // Make sure when returning to this component, the very first page is loaded
    this.dataSource.zoekopdrachtResetToFirstPage();
  }
}
