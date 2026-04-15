/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NgIf } from "@angular/common";
import {
  AfterViewInit,
  Component,
  EventEmitter,
  OnDestroy,
  OnInit,
  ViewChild,
} from "@angular/core";
import { MatAnchor, MatIconButton } from "@angular/material/button";
import { MatDialog } from "@angular/material/dialog";
import { MatIcon } from "@angular/material/icon";
import { MatPaginator } from "@angular/material/paginator";
import {
  MatDrawer,
  MatDrawerContainer,
  MatDrawerContent,
  MatSidenav,
} from "@angular/material/sidenav";
import { MatSort, MatSortHeader } from "@angular/material/sort";
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
import { ActivatedRoute, RouterLink } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { merge } from "rxjs";
import { map, startWith, switchMap } from "rxjs/operators";
import { UtilService } from "../../core/service/util.service";
import { GebruikersvoorkeurenService } from "../../gebruikersvoorkeuren/gebruikersvoorkeuren.service";
import { ZoekFilters } from "../../gebruikersvoorkeuren/zoekopdracht/zoekfilters.model";
import { ZoekopdrachtComponent } from "../../gebruikersvoorkeuren/zoekopdracht/zoekopdracht.component";
import { InformatieObjectenModule } from "../../informatie-objecten/informatie-objecten.module";
import { InformatieObjectenService } from "../../informatie-objecten/informatie-objecten.service";
import {
  ConfirmDialogComponent,
  ConfirmDialogData,
} from "../../shared/confirm-dialog/confirm-dialog.component";
import { WerklijstComponent } from "../../shared/dynamic-table/datasource/werklijst-component";
import { PutBody } from "../../shared/http/http-client";
import { DatumPipe } from "../../shared/pipes/datum.pipe";
import { ReadMoreComponent } from "../../shared/read-more/read-more.component";
import {
  SessionStorageUtil,
  WerklijstZoekParameter,
} from "../../shared/storage/session-storage.util";
import { DateRangeFilterComponent } from "../../shared/table-zoek-filters/date-range-filter/date-range-filter.component";
import { TekstFilterComponent } from "../../shared/table-zoek-filters/tekst-filter/tekst-filter.component";
import { GeneratedType } from "../../shared/utils/generated-types";
import { InboxDocumentenService } from "../inbox-documenten.service";

@Component({
  templateUrl: "./inbox-documenten-list.component.html",
  styleUrls: ["./inbox-documenten-list.component.less"],
  standalone: true,
  imports: [
    NgIf,
    RouterLink,
    MatDrawerContainer,
    MatDrawer,
    MatDrawerContent,
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
    MatAnchor,
    MatIcon,
    TranslateModule,
    TekstFilterComponent,
    DateRangeFilterComponent,
    ReadMoreComponent,
    ZoekopdrachtComponent,
    DatumPipe,
    InformatieObjectenModule,
  ],
})
export class InboxDocumentenListComponent
  extends WerklijstComponent
  implements OnInit, AfterViewInit, OnDestroy
{
  protected isLoadingResults = true;
  protected dataSource = new MatTableDataSource<
    GeneratedType<"RestInboxDocument">
  >();
  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;
  @ViewChild("actionsSidenav") actionsSidenav!: MatSidenav;

  protected readonly displayedColumns = [
    "enkelvoudiginformatieobjectID",
    "creatiedatum",
    "titel",
    "actions",
  ] as const;
  protected readonly filterColumns = [
    "identificatie_filter",
    "creatiedatum_filter",
    "titel_filter",
    "actions_filter",
  ] as const;
  protected listParameters: PutBody<"/rest/inboxdocumenten"> = {};
  protected listParametersSort: {
    sort: keyof PutBody<"/rest/inboxdocumenten">;
    order: "desc" | "asc";
    filtersType: ZoekFilters["filtersType"];
  } = {
    sort: "creatiedatum",
    order: "desc",
    filtersType: "InboxDocumentListParameters",
  };
  protected filterChange = new EventEmitter<void>();
  protected clearZoekopdracht = new EventEmitter<void>();
  protected selectedInformationObject: GeneratedType<"RestInboxDocument"> | null =
    null;

  constructor(
    private readonly inboxDocumentenService: InboxDocumentenService,
    private readonly infoService: InformatieObjectenService,
    private readonly utilService: UtilService,
    private readonly dialog: MatDialog,
    public readonly gebruikersvoorkeurenService: GebruikersvoorkeurenService,
    public readonly route: ActivatedRoute,
  ) {
    super();
  }

  ngOnInit() {
    super.ngOnInit();
    this.utilService.setTitle("title.documenten.inboxDocumenten");
    this.listParameters = SessionStorageUtil.getItem(
      "INBOX_DOCUMENTEN_ZOEKPARAMETERS" satisfies WerklijstZoekParameter,
      this.createDefaultParameters(),
    );
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
          return this.inboxDocumentenService.list({
            ...this.listParametersSort,
            ...this.listParameters,
          });
        }),
        map((data) => {
          this.isLoadingResults = false;
          this.utilService.setLoading(false);
          return data;
        }),
      )
      .subscribe((data) => {
        this.paginator.length = data.totaal ?? 0;
        this.dataSource.data = data.resultaten ?? [];
      });
  }

  updateListParameters() {
    this.listParameters.sort = this.sort.active;
    this.listParameters.order = this.sort.direction;
    this.listParameters.page = this.paginator.pageIndex;
    this.listParameters.maxResults = this.paginator.pageSize;
    SessionStorageUtil.setItem(
      "INBOX_DOCUMENTEN_ZOEKPARAMETERS" satisfies WerklijstZoekParameter,
      this.listParameters,
    );
  }

  getDownloadURL(inboxDocument: GeneratedType<"RestInboxDocument">) {
    if (!inboxDocument.enkelvoudiginformatieobjectUUID) return null;
    return this.infoService.getDownloadURL(
      inboxDocument.enkelvoudiginformatieobjectUUID,
    );
  }

  documentVerwijderen(inboxDocument: GeneratedType<"RestInboxDocument">) {
    this.dialog
      .open(ConfirmDialogComponent, {
        data: new ConfirmDialogData(
          {
            key: "msg.document.verwijderen.bevestigen",
            args: { document: inboxDocument.titel },
          },
          this.inboxDocumentenService.delete(inboxDocument.id!),
        ),
      })
      .afterClosed()
      .subscribe((result) => {
        if (result) {
          this.utilService.openSnackbar("msg.document.verwijderen.uitgevoerd", {
            document: inboxDocument.titel,
          });
          this.filterChange.emit();
        }
      });
  }

  filtersChanged() {
    this.paginator.pageIndex = 0;
    this.clearZoekopdracht.emit();
    this.filterChange.emit();
  }

  resetSearch() {
    this.listParameters = SessionStorageUtil.setItem(
      "INBOX_DOCUMENTEN_ZOEKPARAMETERS" satisfies WerklijstZoekParameter,
      this.createDefaultParameters(),
    );
    this.sort.active = this.listParametersSort.sort;
    this.sort.direction = this.listParametersSort.order;
    this.paginator.pageIndex = 0;
    this.filterChange.emit();
  }

  retriggerSearch() {
    this.filterChange.emit();
  }

  createDefaultParameters() {
    return {
      sort: "creatiedatum",
      order: "desc",
      filtersType: "InboxDocumentListParameters",
    } satisfies typeof this.listParametersSort;
  }

  zoekopdrachtChanged(actieveZoekopdracht: GeneratedType<"RESTZoekopdracht">) {
    if (actieveZoekopdracht?.json) {
      this.listParameters = JSON.parse(actieveZoekopdracht.json);
      this.sort.active = this.listParametersSort.sort;
      this.sort.direction = this.listParametersSort.order;
      this.paginator.pageIndex = 0;
      this.filterChange.emit();
    } else if (actieveZoekopdracht === null) {
      this.resetSearch();
    } else {
      this.filterChange.emit();
    }
  }

  getWerklijst(): GeneratedType<"Werklijst"> {
    return "INBOX_DOCUMENTEN";
  }

  openDrawer(selectedInformationObject: GeneratedType<"RestInboxDocument">) {
    this.selectedInformationObject = selectedInformationObject;
    void this.actionsSidenav.open();
  }

  ngOnDestroy() {
    // Make sure when returning to this component, the very first page is loaded
    this.listParameters.page = 0;
    SessionStorageUtil.setItem(
      "INBOX_DOCUMENTEN_ZOEKPARAMETERS" satisfies WerklijstZoekParameter,
      this.listParameters,
    );
  }
}
