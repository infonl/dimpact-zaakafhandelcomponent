/*
 * SPDX-FileCopyrightText: 2021 - 2022 Atos, 2025 INFO.nl
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
import {
  AfterViewInit,
  Component,
  OnDestroy,
  OnInit,
  ViewChild,
} from "@angular/core";

import { detailExpand } from "../../shared/animations/animations";

import { MatIconAnchor, MatIconButton } from "@angular/material/button";
import { MatIcon } from "@angular/material/icon";
import { MatPaginator } from "@angular/material/paginator";
import { MatSort, MatSortHeader } from "@angular/material/sort";
import { MatTable, MatTableModule } from "@angular/material/table";
import { ActivatedRoute, RouterLink } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
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
import { ZakenAfgehandeldDatasource } from "./zaken-afgehandeld-datasource";

@Component({
  templateUrl: "./zaken-afgehandeld.component.html",
  styleUrls: ["./zaken-afgehandeld.component.less"],
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
export class ZakenAfgehandeldComponent
  extends WerklijstComponent
  implements AfterViewInit, OnInit, OnDestroy
{
  protected dataSource: ZakenAfgehandeldDatasource;
  @ViewChild(MatPaginator) private paginator!: MatPaginator;
  @ViewChild(MatSort) private sort!: MatSort;
  @ViewChild(MatTable) private table!: MatTable<ZaakZoekObject>;
  protected expandedRow: ZaakZoekObject | null = null;
  protected readonly zoekenColumn = ZoekenColumn;
  protected readonly indicatiesLayout = IndicatiesLayout;

  protected einddatumGeplandIcon: TextIcon = new TextIcon(
    DateConditionals.provideFormControlValue(DateConditionals.isExceeded),
    "report_problem",
    "warningVerlopen_icon",
    "msg.datum.overschreden",
    "warning",
  );
  protected uiterlijkeEinddatumAfdoeningIcon: TextIcon = new TextIcon(
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
    protected utilService: UtilService,
  ) {
    super();
    this.dataSource = new ZakenAfgehandeldDatasource(
      this.zoekenService,
      this.utilService,
    );
  }

  ngOnInit(): void {
    super.ngOnInit();
    this.utilService.setTitle("title.zaken.afgehandeld");
    this.dataSource.initColumns(this.defaultColumns());
  }

  protected defaultColumns(): Map<ZoekenColumn, ColumnPickerValue> {
    return new Map([
      [ZoekenColumn.ZAAK_DOT_IDENTIFICATIE, ColumnPickerValue.VISIBLE],
      [ZoekenColumn.STATUS, ColumnPickerValue.HIDDEN],
      [ZoekenColumn.ZAAKTYPE, ColumnPickerValue.VISIBLE],
      [ZoekenColumn.OMSCHRIJVING, ColumnPickerValue.VISIBLE],
      [ZoekenColumn.GROEP, ColumnPickerValue.HIDDEN],
      [ZoekenColumn.COMMUNICATIEKANAAL, ColumnPickerValue.HIDDEN],
      [ZoekenColumn.VERTROUWELIJKHEIDAANDUIDING, ColumnPickerValue.HIDDEN],
      [ZoekenColumn.STARTDATUM, ColumnPickerValue.VISIBLE],
      [ZoekenColumn.REGISTRATIEDATUM, ColumnPickerValue.HIDDEN],
      [ZoekenColumn.EINDDATUM, ColumnPickerValue.VISIBLE],
      [ZoekenColumn.EINDDATUM_GEPLAND, ColumnPickerValue.HIDDEN],
      [ZoekenColumn.BEHANDELAAR, ColumnPickerValue.VISIBLE],
      [ZoekenColumn.UITERLIJKE_EINDDATUM_AFDOENING, ColumnPickerValue.HIDDEN],
      [ZoekenColumn.ARCHIEF_ACTIEDATUM, ColumnPickerValue.HIDDEN],
      [ZoekenColumn.ARCHIEF_NOMINATIE, ColumnPickerValue.HIDDEN],
      [ZoekenColumn.TOELICHTING, ColumnPickerValue.HIDDEN],
      [ZoekenColumn.RESULTAAT, ColumnPickerValue.VISIBLE],
      [ZoekenColumn.INDICATIES, ColumnPickerValue.HIDDEN],
      [ZoekenColumn.URL, ColumnPickerValue.STICKY],
    ]);
  }

  getWerklijst(): GeneratedType<"Werklijst"> {
    return "AFGEHANDELDE_ZAKEN";
  }

  ngAfterViewInit(): void {
    this.dataSource.setViewChilds(this.paginator, this.sort);
    this.table.dataSource = this.dataSource;
  }

  protected isAfterDateLimit(
    date: Date | moment.Moment | string,
    dateLimit: Date | moment.Moment | string,
  ): boolean {
    return DateConditionals.isExceeded(date, dateLimit);
  }

  protected resetColumns(): void {
    this.dataSource.resetColumns();
  }

  protected filtersChange(): void {
    this.dataSource.filtersChanged();
  }

  ngOnDestroy(): void {
    // Make sure when returning to this comnponent, the very first page is loaded
    this.dataSource.zoekopdrachtResetToFirstPage();
  }
}
