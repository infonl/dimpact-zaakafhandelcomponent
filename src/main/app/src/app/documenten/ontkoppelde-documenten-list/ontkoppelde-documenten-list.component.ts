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
import { Werklijst } from "../../gebruikersvoorkeuren/model/werklijst";
import { Zoekopdracht } from "../../gebruikersvoorkeuren/model/zoekopdracht";
import { ZoekFilters } from "../../gebruikersvoorkeuren/zoekopdracht/zoekfilters.model";
import { InformatieObjectenService } from "../../informatie-objecten/informatie-objecten.service";
import {
  ConfirmDialogComponent,
  ConfirmDialogData,
} from "../../shared/confirm-dialog/confirm-dialog.component";
import { WerklijstComponent } from "../../shared/dynamic-table/datasource/werklijst-component";
import { PutBody } from "../../shared/http/zac-http-client";
import { SessionStorageUtil } from "../../shared/storage/session-storage.util";
import { GeneratedType } from "../../shared/utils/generated-types";
import { OntkoppeldeDocumentenService } from "../ontkoppelde-documenten.service";

@Component({
  templateUrl: "./ontkoppelde-documenten-list.component.html",
  styleUrls: ["./ontkoppelde-documenten-list.component.less"],
})
export class OntkoppeldeDocumentenListComponent
  extends WerklijstComponent
  implements OnInit, AfterViewInit, OnDestroy
{
  isLoadingResults = true;
  dataSource = new MatTableDataSource<
    GeneratedType<"RESTOntkoppeldDocument">
  >();
  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;
  @ViewChild("actionsSidenav") actionsSidenav!: MatSidenav;

  displayedColumns = [
    "titel",
    "creatiedatum",
    "zaakID",
    "ontkoppeldDoor",
    "ontkoppeldOp",
    "reden",
    "actions",
  ] as const;
  filterColumns = [
    "titel_filter",
    "creatiedatum_filter",
    "zaakID_filter",
    "ontkoppeldDoor_filter",
    "ontkoppeldOp_filter",
    "reden_filter",
    "actions_filter",
  ] as const;
  listParameters: PutBody<"/rest/ontkoppeldedocumenten"> = {};
  listParametersSort: {
    sort: keyof PutBody<"/rest/ontkoppeldedocumenten">;
    order: "desc" | "asc";
    filtersType: ZoekFilters["filtersType"];
  } = {
    sort: "ontkoppeldOp",
    order: "desc",
    filtersType: "OntkoppeldDocumentListParameters",
  };
  filterOntkoppeldDoor: GeneratedType<"RestUser">[] = [];
  filterChange = new EventEmitter<void>();
  clearZoekopdracht = new EventEmitter<void>();
  selectedInformationObject: GeneratedType<"RESTOntkoppeldDocument"> | null =
    null;

  constructor(
    private ontkoppeldeDocumentenService: OntkoppeldeDocumentenService,
    private infoService: InformatieObjectenService,
    private utilService: UtilService,
    public dialog: MatDialog,
    public gebruikersvoorkeurenService: GebruikersvoorkeurenService,
    public route: ActivatedRoute,
  ) {
    super();
  }

  ngOnInit(): void {
    super.ngOnInit();
    this.utilService.setTitle("title.documenten.ontkoppeldeDocumenten");
    this.listParameters = SessionStorageUtil.getItem(
      Werklijst.ONTKOPPELDE_DOCUMENTEN + "_ZOEKPARAMETERS",
      this.createDefaultParameters(),
    );
  }

  ngAfterViewInit(): void {
    this.sort.sortChange.subscribe(() => (this.paginator.pageIndex = 0));
    merge(this.sort.sortChange, this.paginator.page, this.filterChange)
      .pipe(
        startWith({}),
        switchMap(() => {
          this.isLoadingResults = true;
          this.utilService.setLoading(true);
          this.updateListParameters();
          return this.ontkoppeldeDocumentenService.list({
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
        this.filterOntkoppeldDoor =
          data.resultaten?.map((result) => result.ontkoppeldDoor!) ?? [];
        this.dataSource.data = data.resultaten ?? [];
      });
  }

  openDrawer(
    selectedInformationObject: GeneratedType<"RESTOntkoppeldDocument">,
  ) {
    this.selectedInformationObject = selectedInformationObject;
    void this.actionsSidenav.open();
  }

  updateListParameters(): void {
    this.listParameters.sort = this.sort.active;
    this.listParameters.order = this.sort.direction;
    this.listParameters.page = this.paginator.pageIndex;
    this.listParameters.maxResults = this.paginator.pageSize;
    SessionStorageUtil.setItem(
      Werklijst.ONTKOPPELDE_DOCUMENTEN + "_ZOEKPARAMETERS",
      this.listParameters,
    );
  }

  getDownloadURL(ontkoppeldDocument: GeneratedType<"RESTOntkoppeldDocument">) {
    if (!ontkoppeldDocument.documentUUID) return null;
    return this.infoService.getDownloadURL(ontkoppeldDocument.documentUUID);
  }

  documentVerwijderen(
    ontkoppeldDocument: GeneratedType<"RESTOntkoppeldDocument">,
  ) {
    this.dialog
      .open(ConfirmDialogComponent, {
        data: new ConfirmDialogData(
          {
            key: "msg.document.verwijderen.bevestigen",
            args: { document: ontkoppeldDocument.titel },
          },
          this.ontkoppeldeDocumentenService.delete(ontkoppeldDocument.id!),
        ),
      })
      .afterClosed()
      .subscribe((result) => {
        if (result) {
          this.utilService.openSnackbar("msg.document.verwijderen.uitgevoerd", {
            document: ontkoppeldDocument.titel,
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

  resetSearch(): void {
    this.listParameters = SessionStorageUtil.setItem(
      Werklijst.ONTKOPPELDE_DOCUMENTEN + "_ZOEKPARAMETERS",
      this.createDefaultParameters(),
    );
    this.sort.active = this.listParametersSort.sort;
    this.sort.direction = this.listParametersSort.order;
    this.paginator.pageIndex = 0;
    this.filterChange.emit();
  }

  zoekopdrachtChanged(actieveZoekopdracht: Zoekopdracht): void {
    if (actieveZoekopdracht) {
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

  retriggerSearch(): void {
    this.filterChange.emit();
  }

  createDefaultParameters() {
    return {
      sort: "ontkoppeldOp",
      order: "desc",
      filtersType: "OntkoppeldDocumentListParameters",
    } satisfies typeof this.listParametersSort;
  }

  compareUser = (
    user1: GeneratedType<"RestUser">,
    user2: GeneratedType<"RestUser">,
  ) => {
    return user1?.id === user2?.id;
  };

  getWerklijst() {
    return Werklijst.ONTKOPPELDE_DOCUMENTEN;
  }

  ngOnDestroy(): void {
    // Make sure when returning to this component, the very first page is loaded
    this.listParameters.page = 0;
    SessionStorageUtil.setItem(
      Werklijst.ONTKOPPELDE_DOCUMENTEN + "_ZOEKPARAMETERS",
      this.listParameters,
    );
  }
}
