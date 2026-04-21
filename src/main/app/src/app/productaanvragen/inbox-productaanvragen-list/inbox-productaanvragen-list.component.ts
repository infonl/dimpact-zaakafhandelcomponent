/*
 * SPDX-FileCopyrightText: 2023 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NgFor, NgIf } from "@angular/common";
import {
  AfterViewInit,
  Component,
  EventEmitter,
  OnDestroy,
  OnInit,
  ViewChild,
} from "@angular/core";
import { MatIconAnchor, MatIconButton } from "@angular/material/button";
import { MatDialog } from "@angular/material/dialog";
import { MatFormField } from "@angular/material/form-field";
import { MatIcon } from "@angular/material/icon";
import { MatPaginator } from "@angular/material/paginator";
import {
  MatOption,
  MatSelect,
  MatSelectChange,
} from "@angular/material/select";
import { MatSort, MatSortHeader, SortDirection } from "@angular/material/sort";
import {
  MatCell,
  MatCellDef,
  MatColumnDef,
  MatHeaderCell,
  MatHeaderCellDef,
  MatHeaderRow,
  MatHeaderRowDef,
  MatRow,
  MatRowDef,
  MatTable,
  MatTableDataSource,
} from "@angular/material/table";
import { DomSanitizer, SafeUrl } from "@angular/platform-browser";
import { ActivatedRoute, Router, RouterLink } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { merge } from "rxjs";
import { map, startWith, switchMap } from "rxjs/operators";
import { UtilService } from "../../core/service/util.service";
import { GebruikersvoorkeurenService } from "../../gebruikersvoorkeuren/gebruikersvoorkeuren.service";
import { ZoekopdrachtComponent } from "../../gebruikersvoorkeuren/zoekopdracht/zoekopdracht.component";
import { InformatieObjectenService } from "../../informatie-objecten/informatie-objecten.service";
import { detailExpand } from "../../shared/animations/animations";
import {
  ConfirmDialogComponent,
  ConfirmDialogData,
} from "../../shared/confirm-dialog/confirm-dialog.component";
import { WerklijstComponent } from "../../shared/dynamic-table/datasource/werklijst-component";
import { DatumPipe } from "../../shared/pipes/datum.pipe";
import {
  SessionStorageUtil,
  WerklijstZoekParameter,
} from "../../shared/storage/session-storage.util";
import { DateRangeFilterComponent } from "../../shared/table-zoek-filters/date-range-filter/date-range-filter.component";
import { TekstFilterComponent } from "../../shared/table-zoek-filters/tekst-filter/tekst-filter.component";
import { GeneratedType } from "../../shared/utils/generated-types";
import { DatumRange } from "../../zoeken/model/datum-range";
import { InboxProductaanvragenService } from "../inbox-productaanvragen.service";

@Component({
  templateUrl: "./inbox-productaanvragen-list.component.html",
  styleUrls: ["./inbox-productaanvragen-list.component.less"],
  animations: [detailExpand],
  standalone: true,
  imports: [
    NgIf,
    NgFor,
    RouterLink,
    MatTable,
    MatColumnDef,
    MatHeaderCellDef,
    MatHeaderCell,
    MatCellDef,
    MatCell,
    MatHeaderRowDef,
    MatHeaderRow,
    MatRowDef,
    MatRow,
    MatSort,
    MatSortHeader,
    MatPaginator,
    MatIconButton,
    MatIconAnchor,
    MatIcon,
    MatFormField,
    MatSelect,
    MatOption,
    TranslateModule,
    TekstFilterComponent,
    DateRangeFilterComponent,
    ZoekopdrachtComponent,
    DatumPipe,
  ],
})
export class InboxProductaanvragenListComponent
  extends WerklijstComponent
  implements OnInit, AfterViewInit, OnDestroy
{
  protected isLoadingResults = true;
  protected dataSource = new MatTableDataSource<
    GeneratedType<"RestInboxProductaanvraag">
  >();
  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;
  protected readonly displayedColumns = [
    "expand",
    "type",
    "ontvangstdatum",
    "initiator",
    "aantal_bijlagen",
    "actions",
  ] as const;
  protected readonly filterColumns = [
    "expand_filter",
    "type_filter",
    "ontvangstdatum_filter",
    "initiator_filter",
    "aantal_bijlagen_filter",
    "actions_filter",
  ] as const;
  protected listParameters = SessionStorageUtil.getItem(
    `${this.getWerklijst()}_ZOEKPARAMETERS` satisfies WerklijstZoekParameter,
    this.createDefaultParameters(),
  );
  protected expandedRow: GeneratedType<"RestInboxProductaanvraag"> | null =
    null;
  protected filterType: string[] = [];
  protected filterChange = new EventEmitter<void>();
  protected clearZoekopdracht = new EventEmitter<void>();
  protected previewSrc: SafeUrl | null = null;

  constructor(
    private readonly inboxProductaanvragenService: InboxProductaanvragenService,
    private readonly infoService: InformatieObjectenService,
    private readonly utilService: UtilService,
    private readonly dialog: MatDialog,
    public readonly gebruikersvoorkeurenService: GebruikersvoorkeurenService,
    public readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly sanitizer: DomSanitizer,
  ) {
    super();
  }

  ngOnInit() {
    super.ngOnInit();
    this.utilService.setTitle("title.productaanvragen.inboxProductaanvragen");
  }

  ngAfterViewInit() {
    this.sort.sortChange.subscribe(() => (this.paginator.pageIndex = 0));
    merge(this.sort.sortChange, this.paginator.page, this.filterChange)
      .pipe(
        startWith({}),
        switchMap(() => {
          this.isLoadingResults = true;
          this.utilService.setLoading(true);
          this.updateListParameters();
          return this.inboxProductaanvragenService.list(this.listParameters);
        }),
        map((data) => {
          this.isLoadingResults = false;
          this.utilService.setLoading(false);
          return data;
        }),
      )
      .subscribe((data) => {
        this.paginator.length = Number(data.totaal);
        this.filterType = (data as { filterType: string[] }).filterType;
        this.dataSource.data = data.resultaten ?? [];
      });
  }

  protected updateListParameters() {
    this.listParameters.sort = this.sort.active;
    this.listParameters.order = this.sort.direction;
    this.listParameters.page = this.paginator.pageIndex;
    this.listParameters.maxResults = this.paginator.pageSize;
    SessionStorageUtil.setItem(
      `${this.getWerklijst()}_ZOEKPARAMETERS` satisfies WerklijstZoekParameter,
      this.listParameters,
    );
  }

  protected getDownloadURL(ip: GeneratedType<"RestInboxProductaanvraag">) {
    return this.infoService.getDownloadURL(ip.aanvraagdocumentUUID!);
  }

  protected filtersChanged(options: {
    event: MatSelectChange | string | DatumRange;
    filter: keyof GeneratedType<"RestInboxProductaanvraagListParameters">;
  }) {
    this.listParameters[options.filter] =
      typeof options.event === "object" && "value" in options.event
        ? (options.event.value as never)
        : (undefined as never);
    this.paginator.pageIndex = 0;
    this.clearZoekopdracht.emit();
    this.filterChange.emit();
  }

  protected resetSearch() {
    this.listParameters = SessionStorageUtil.setItem(
      `${this.getWerklijst()}_ZOEKPARAMETERS` satisfies WerklijstZoekParameter,
      this.createDefaultParameters(),
    );
    this.sort.active = this.listParameters.sort ?? "id";
    this.sort.direction = this.listParameters.order as SortDirection;
    this.paginator.pageIndex = 0;
    this.filterChange.emit();
  }

  protected zoekopdrachtChanged(
    actieveZoekopdracht: GeneratedType<"RESTZoekopdracht">,
  ) {
    if (actieveZoekopdracht?.json) {
      this.listParameters = JSON.parse(actieveZoekopdracht.json);
      this.sort.active = this.listParameters.sort ?? "id";
      this.sort.direction = this.listParameters.order as SortDirection;
      this.paginator.pageIndex = 0;
      this.filterChange.emit();
    } else if (actieveZoekopdracht === null) {
      this.resetSearch();
    } else {
      this.filterChange.emit();
    }
  }

  protected createDefaultParameters(): GeneratedType<"RestInboxProductaanvraagListParameters"> {
    return { sort: "id", order: "desc" };
  }

  getWerklijst(): GeneratedType<"Werklijst"> {
    return "INBOX_PRODUCTAANVRAGEN";
  }

  protected updateActive(
    selectedRow: GeneratedType<"RestInboxProductaanvraag">,
  ) {
    if (this.expandedRow === selectedRow) {
      this.expandedRow = null;
      this.previewSrc = null;
    } else {
      this.expandedRow = selectedRow;
      this.previewSrc = this.sanitizer.bypassSecurityTrustResourceUrl(
        this.inboxProductaanvragenService.pdfPreview(
          selectedRow.aanvraagdocumentUUID!,
        ),
      );
    }
  }

  protected aanmakenZaak(
    inboxProductaanvraag: GeneratedType<"RestInboxProductaanvraag">,
  ) {
    this.router.navigateByUrl("zaken/create", {
      state: { inboxProductaanvraag },
    });
  }

  protected inboxProductaanvragenVerwijderen(
    inboxProductaanvraag: GeneratedType<"RestInboxProductaanvraag">,
  ) {
    this.dialog
      .open(ConfirmDialogComponent, {
        data: new ConfirmDialogData(
          "msg.inboxProductaanvraag.verwijderen.bevestigen",
          this.inboxProductaanvragenService.delete(
            Number(inboxProductaanvraag.id),
          ),
        ),
      })
      .afterClosed()
      .subscribe((result) => {
        if (result) {
          this.utilService.openSnackbar(
            "msg.inboxProductaanvraag.verwijderen.uitgevoerd",
          );
          this.filterChange.emit();
        }
      });
  }

  ngOnDestroy() {
    // Make sure when returning to this comnponent, the very first page is loaded
    this.listParameters.page = 0;
    SessionStorageUtil.setItem(
      `${this.getWerklijst()}_ZOEKPARAMETERS` satisfies WerklijstZoekParameter,
      this.listParameters,
    );
  }
}
