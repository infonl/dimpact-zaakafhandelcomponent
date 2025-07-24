/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  AfterViewInit,
  Component,
  EventEmitter,
  OnDestroy,
  OnInit,
  ViewChild,
} from "@angular/core";
import { MatDialog } from "@angular/material/dialog";
import { MatPaginator } from "@angular/material/paginator";
import { MatSidenav } from "@angular/material/sidenav";
import { MatSort } from "@angular/material/sort";
import { MatTableDataSource } from "@angular/material/table";
import { ActivatedRoute } from "@angular/router";
import { merge } from "rxjs";
import { map, startWith, switchMap } from "rxjs/operators";
import { UtilService } from "../../core/service/util.service";
import { GebruikersvoorkeurenService } from "../../gebruikersvoorkeuren/gebruikersvoorkeuren.service";
import { ZoekFilters } from "../../gebruikersvoorkeuren/zoekopdracht/zoekfilters.model";
import { InformatieObjectenService } from "../../informatie-objecten/informatie-objecten.service";
import {
  ConfirmDialogComponent,
  ConfirmDialogData,
} from "../../shared/confirm-dialog/confirm-dialog.component";
import { WerklijstComponent } from "../../shared/dynamic-table/datasource/werklijst-component";
import { PutBody } from "../../shared/http/zac-http-client";
import {
  SessionStorageUtil,
  WerklijstZoekParameter,
} from "../../shared/storage/session-storage.util";
import { GeneratedType } from "../../shared/utils/generated-types";
import { InboxDocumentenService } from "../inbox-documenten.service";

@Component({
  templateUrl: "./inbox-documenten-list.component.html",
  styleUrls: ["./inbox-documenten-list.component.less"],
})
export class InboxDocumentenListComponent
  extends WerklijstComponent
  implements OnInit, AfterViewInit, OnDestroy
{
  isLoadingResults = true;
  dataSource = new MatTableDataSource<GeneratedType<"RESTInboxDocument">>();
  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;
  @ViewChild("actionsSidenav") actionsSidenav!: MatSidenav;

  displayedColumns = [
    "enkelvoudiginformatieobjectID",
    "creatiedatum",
    "titel",
    "actions",
  ] as const;
  filterColumns = [
    "identificatie_filter",
    "creatiedatum_filter",
    "titel_filter",
    "actions_filter",
  ] as const;
  listParameters: PutBody<"/rest/inboxdocumenten"> = {};
  listParametersSort: {
    sort: keyof PutBody<"/rest/inboxdocumenten">;
    order: "desc" | "asc";
    filtersType: ZoekFilters["filtersType"];
  } = {
    sort: "creatiedatum",
    order: "desc",
    filtersType: "InboxDocumentListParameters",
  };
  filterChange = new EventEmitter<void>();
  clearZoekopdracht = new EventEmitter<void>();
  selectedInformationObject: GeneratedType<"RESTOntkoppeldDocument"> | null =
    null;

  constructor(
    private readonly inboxDocumentenService: InboxDocumentenService,
    private readonly infoService: InformatieObjectenService,
    private readonly utilService: UtilService,
    public readonly dialog: MatDialog,
    public readonly gebruikersvoorkeurenService: GebruikersvoorkeurenService,
    public readonly route: ActivatedRoute,
  ) {
    super();
  }

  ngOnInit() {
    super.ngOnInit();
    this.listParametersSort = SessionStorageUtil.getItem(
      "INBOX_DOCUMENTEN_ZOEKPARAMETERS" satisfies WerklijstZoekParameter,
      this.createDefaultParameters(),
    );
    this.utilService.setTitle("title.documenten.inboxDocumenten");
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
            ...this.listParameters,
            ...this.listParametersSort,
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
    this.listParameters.sort = this.sort.active ?? "creatiedatum";
    this.listParameters.order = this.sort.direction ?? "desc";
    this.listParameters.page = this.paginator.pageIndex;
    this.listParameters.maxResults = this.paginator.pageSize;
    SessionStorageUtil.setItem(
      "INBOX_DOCUMENTEN_ZOEKPARAMETERS" satisfies WerklijstZoekParameter,
      this.listParameters,
    );
  }

  getDownloadURL(inboxDocument: GeneratedType<"RESTInboxDocument">) {
    if (!inboxDocument.enkelvoudiginformatieobjectUUID) return null;
    return this.infoService.getDownloadURL(
      inboxDocument.enkelvoudiginformatieobjectUUID,
    );
  }

  documentVerwijderen(inboxDocument: GeneratedType<"RESTInboxDocument">) {
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
          this.paginator.page.emit();
        }
      });
  }

  filtersChanged() {
    this.paginator.pageIndex = 0;
    this.clearZoekopdracht.emit();
    this.filterChange.emit();
  }

  resetSearch() {
    this.listParametersSort = SessionStorageUtil.setItem(
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

  openDrawer(
    selectedInformationObject: GeneratedType<"RESTOntkoppeldDocument">,
  ) {
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
