/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 Lifely
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
import { MatSelectChange } from "@angular/material/select";
import { MatSort } from "@angular/material/sort";
import { MatTableDataSource } from "@angular/material/table";
import { ActivatedRoute } from "@angular/router";
import { merge } from "rxjs";
import { map, startWith, switchMap } from "rxjs/operators";
import { UtilService } from "../../core/service/util.service";
import { GebruikersvoorkeurenService } from "../../gebruikersvoorkeuren/gebruikersvoorkeuren.service";
import { Werklijst } from "../../gebruikersvoorkeuren/model/werklijst";
import { Zoekopdracht } from "../../gebruikersvoorkeuren/model/zoekopdracht";
import { InformatieObjectVerplaatsService } from "../../informatie-objecten/informatie-object-verplaats.service";
import { InformatieObjectenService } from "../../informatie-objecten/informatie-objecten.service";
import {
  ConfirmDialogComponent,
  ConfirmDialogData,
} from "../../shared/confirm-dialog/confirm-dialog.component";
import { WerklijstComponent } from "../../shared/dynamic-table/datasource/werklijst-component";
import { SessionStorageUtil } from "../../shared/storage/session-storage.util";
import { GeneratedType } from "../../shared/utils/generated-types";
import { DatumRange } from "../../zoeken/model/datum-range";
import { OntkoppeldDocument } from "../model/ontkoppeld-document";
import { OntkoppeldDocumentListParameters } from "../model/ontkoppeld-document-list-parameters";
import { OntkoppeldeDocumentenService } from "../ontkoppelde-documenten.service";
import { MatDrawer, MatSidenav } from "@angular/material/sidenav";

@Component({
  templateUrl: "./ontkoppelde-documenten-list.component.html",
  styleUrls: ["./ontkoppelde-documenten-list.component.less"],
})
export class OntkoppeldeDocumentenListComponent
  extends WerklijstComponent
  implements OnInit, AfterViewInit, OnDestroy
{
  isLoadingResults = true;
  dataSource = new MatTableDataSource<OntkoppeldDocument>();
  @ViewChild(MatPaginator) paginator: MatPaginator;
  @ViewChild(MatSort) sort: MatSort;
  @ViewChild("actionsSidenav") actionsSidenav!: MatSidenav;

  displayedColumns: string[] = [
    "titel",
    "creatiedatum",
    "zaakID",
    "ontkoppeldDoor",
    "ontkoppeldOp",
    "reden",
    "actions",
  ];
  filterColumns: string[] = [
    "titel_filter",
    "creatiedatum_filter",
    "zaakID_filter",
    "ontkoppeldDoor_filter",
    "ontkoppeldOp_filter",
    "reden_filter",
    "actions_filter",
  ];
  listParameters: OntkoppeldDocumentListParameters;
  filterOntkoppeldDoor: GeneratedType<"RestUser">[] = [];
  filterChange = new EventEmitter<void>();
  clearZoekopdracht = new EventEmitter<void>();
  selectedDocument: OntkoppeldDocument | null = null;

  constructor(
    private ontkoppeldeDocumentenService: OntkoppeldeDocumentenService,
    private infoService: InformatieObjectenService,
    private utilService: UtilService,
    public dialog: MatDialog,
    private informatieObjectVerplaatsService: InformatieObjectVerplaatsService,
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
          return this.ontkoppeldeDocumentenService.list(this.listParameters);
        }),
        map((data) => {
          this.isLoadingResults = false;
          this.utilService.setLoading(false);
          return data;
        }),
      )
      .subscribe((data) => {
        this.paginator.length = data.totaal;
        this.filterOntkoppeldDoor = data.filterOntkoppeldDoor;
        this.dataSource.data = data.resultaten;
      });
  }

  openDrawer(selectedDocument: OntkoppeldDocument) {
    this.selectedDocument = { ...selectedDocument };

    console.log("this.selectedDocument", this.selectedDocument);
    this.actionsSidenav.open();
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

  getDownloadURL(od: OntkoppeldDocument): string {
    return this.infoService.getDownloadURL(od.documentUUID);
  }

  documentVerplaatsen(od: OntkoppeldDocument): void {
    od["disabled"] = true;
    this.infoService
      .readEnkelvoudigInformatieobject(od.documentUUID)
      .subscribe((i) => {
        this.informatieObjectVerplaatsService.addTeVerplaatsenDocument(
          i,
          "ontkoppelde-documenten",
        );
      });
  }

  documentVerwijderen(od: OntkoppeldDocument): void {
    this.dialog
      .open(ConfirmDialogComponent, {
        data: new ConfirmDialogData(
          {
            key: "msg.document.verwijderen.bevestigen",
            args: { document: od.titel },
          },
          this.ontkoppeldeDocumentenService.delete(od),
        ),
      })
      .afterClosed()
      .subscribe((result) => {
        if (result) {
          this.utilService.openSnackbar("msg.document.verwijderen.uitgevoerd", {
            document: od.titel,
          });
          this.filterChange.emit();
        }
      });
  }

  isDocumentVerplaatsenDisabled(od: OntkoppeldDocument): boolean {
    return (
      od["disabled"] ||
      this.informatieObjectVerplaatsService.isReedsTeVerplaatsen(
        od.documentUUID,
      )
    );
  }

  filtersChanged(options: {
    event: string | MatSelectChange | DatumRange;
    filter: keyof typeof this.listParameters;
  }): void {
    this.listParameters[options.filter] =
      typeof options.event === "object" && "value" in options.event
        ? options.event.value
        : null;
    this.paginator.pageIndex = 0;
    this.clearZoekopdracht.emit();
    this.filterChange.emit();
  }

  resetSearch(): void {
    this.listParameters = SessionStorageUtil.setItem(
      Werklijst.ONTKOPPELDE_DOCUMENTEN + "_ZOEKPARAMETERS",
      this.createDefaultParameters(),
    );
    this.sort.active = this.listParameters.sort;
    this.sort.direction = this.listParameters.order;
    this.paginator.pageIndex = 0;
    this.filterChange.emit();
  }

  zoekopdrachtChanged(actieveZoekopdracht: Zoekopdracht): void {
    if (actieveZoekopdracht) {
      this.listParameters = JSON.parse(actieveZoekopdracht.json);
      this.sort.active = this.listParameters.sort;
      this.sort.direction = this.listParameters.order;
      this.paginator.pageIndex = 0;
      this.filterChange.emit();
    } else if (actieveZoekopdracht === null) {
      this.resetSearch();
    } else {
      this.filterChange.emit();
    }
  }

  createDefaultParameters(): OntkoppeldDocumentListParameters {
    return new OntkoppeldDocumentListParameters("ontkoppeldOp", "desc");
  }

  compareUser = (
    user1: GeneratedType<"RestUser">,
    user2: GeneratedType<"RestUser">,
  ) => {
    return user1?.id === user2?.id;
  };

  getWerklijst(): Werklijst {
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
