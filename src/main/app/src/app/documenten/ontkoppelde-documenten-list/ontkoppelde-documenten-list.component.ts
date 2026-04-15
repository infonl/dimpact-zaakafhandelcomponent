/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
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
import { MatAnchor, MatIconButton } from "@angular/material/button";
import { MatDialog } from "@angular/material/dialog";
import { MatFormField } from "@angular/material/form-field";
import { MatIcon } from "@angular/material/icon";
import { MatPaginator } from "@angular/material/paginator";
import { MatOption, MatSelect } from "@angular/material/select";
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
import { OntkoppeldeDocumentenService } from "../ontkoppelde-documenten.service";

@Component({
  templateUrl: "./ontkoppelde-documenten-list.component.html",
  styleUrls: ["./ontkoppelde-documenten-list.component.less"],
  standalone: true,
  imports: [
    NgIf,
    NgFor,
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
    MatFormField,
    MatSelect,
    MatOption,
    TranslateModule,
    TekstFilterComponent,
    DateRangeFilterComponent,
    ReadMoreComponent,
    ZoekopdrachtComponent,
    DatumPipe,
    InformatieObjectenModule,
  ],
})
export class OntkoppeldeDocumentenListComponent
  extends WerklijstComponent
  implements OnInit, AfterViewInit, OnDestroy
{
  protected isLoadingResults = true;
  protected dataSource = new MatTableDataSource<
    GeneratedType<"RestDetachedDocument">
  >();
  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;
  @ViewChild("actionsSidenav") actionsSidenav!: MatSidenav;

  protected readonly displayedColumns = [
    "titel",
    "creatiedatum",
    "zaakID",
    "ontkoppeldDoor",
    "ontkoppeldOp",
    "reden",
    "actions",
  ] as const;
  protected readonly filterColumns = [
    "titel_filter",
    "creatiedatum_filter",
    "zaakID_filter",
    "ontkoppeldDoor_filter",
    "ontkoppeldOp_filter",
    "reden_filter",
    "actions_filter",
  ] as const;
  protected listParameters: PutBody<"/rest/ontkoppeldedocumenten"> = {};
  protected readonly listParametersSort: {
    sort: keyof PutBody<"/rest/ontkoppeldedocumenten">;
    order: "desc" | "asc";
    filtersType: ZoekFilters["filtersType"];
  } = {
    sort: "ontkoppeldOp",
    order: "desc",
    filtersType: "DetachedDocumentListParameters",
  };
  protected filterOntkoppeldDoor: GeneratedType<"RestUser">[] = [];
  protected filterChange = new EventEmitter<void>();
  protected clearZoekopdracht = new EventEmitter<void>();
  protected selectedInformationObject: GeneratedType<"RestDetachedDocument"> | null =
    null;

  constructor(
    private readonly ontkoppeldeDocumentenService: OntkoppeldeDocumentenService,
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
    this.utilService.setTitle("title.documenten.ontkoppeldeDocumenten");
    this.listParameters = SessionStorageUtil.getItem(
      "ONTKOPPELDE_DOCUMENTEN_ZOEKPARAMETERS" satisfies WerklijstZoekParameter,
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
          data.resultaten?.map(
            (result: GeneratedType<"RestDetachedDocument">) =>
              result.ontkoppeldDoor!,
          ) ?? [];
        this.dataSource.data = data.resultaten ?? [];
      });
  }

  protected openDrawer(
    selectedInformationObject: GeneratedType<"RestDetachedDocument">,
  ) {
    this.selectedInformationObject = selectedInformationObject;
    void this.actionsSidenav.open();
  }

  protected updateListParameters() {
    this.listParameters.sort = this.sort.active;
    this.listParameters.order = this.sort.direction;
    this.listParameters.page = this.paginator.pageIndex;
    this.listParameters.maxResults = this.paginator.pageSize;
    SessionStorageUtil.setItem(
      "ONTKOPPELDE_DOCUMENTEN_ZOEKPARAMETERS" satisfies WerklijstZoekParameter,
      this.listParameters,
    );
  }

  protected getDownloadURL(
    detachedDocument: GeneratedType<"RestDetachedDocument">,
  ) {
    if (!detachedDocument.documentUUID) return null;
    return this.infoService.getDownloadURL(detachedDocument.documentUUID);
  }

  protected documentVerwijderen(
    detachedDocument: GeneratedType<"RestDetachedDocument">,
  ) {
    this.dialog
      .open(ConfirmDialogComponent, {
        data: new ConfirmDialogData(
          {
            key: "msg.document.verwijderen.bevestigen",
            args: { document: detachedDocument.titel },
          },
          this.ontkoppeldeDocumentenService.delete(detachedDocument.id!),
        ),
      })
      .afterClosed()
      .subscribe((result) => {
        if (result) {
          this.utilService.openSnackbar("msg.document.verwijderen.uitgevoerd", {
            document: detachedDocument.titel,
          });
          this.filterChange.emit();
        }
      });
  }

  protected filtersChanged() {
    this.paginator.pageIndex = 0;
    this.clearZoekopdracht.emit();
    this.filterChange.emit();
  }

  protected resetSearch() {
    this.listParameters = SessionStorageUtil.setItem(
      "ONTKOPPELDE_DOCUMENTEN_ZOEKPARAMETERS" satisfies WerklijstZoekParameter,
      this.createDefaultParameters(),
    );
    this.sort.active = this.listParametersSort.sort;
    this.sort.direction = this.listParametersSort.order;
    this.paginator.pageIndex = 0;
    this.filterChange.emit();
  }

  protected zoekopdrachtChanged(
    actieveZoekopdracht: GeneratedType<"RESTZoekopdracht">,
  ) {
    if (actieveZoekopdracht) {
      this.listParameters = JSON.parse(actieveZoekopdracht.json || "{}");
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

  protected retriggerSearch() {
    this.filterChange.emit();
  }

  protected createDefaultParameters() {
    return {
      sort: "ontkoppeldOp",
      order: "desc",
      filtersType: "DetachedDocumentListParameters",
    } satisfies typeof this.listParametersSort;
  }

  protected readonly compareUser = (
    user1: GeneratedType<"RestUser">,
    user2: GeneratedType<"RestUser">,
  ) => {
    return user1?.id === user2?.id;
  };

  getWerklijst(): GeneratedType<"Werklijst"> {
    return "ONTKOPPELDE_DOCUMENTEN";
  }

  ngOnDestroy() {
    // Make sure when returning to this component, the very first page is loaded
    this.listParameters.page = 0;
    SessionStorageUtil.setItem(
      "ONTKOPPELDE_DOCUMENTEN_ZOEKPARAMETERS" satisfies WerklijstZoekParameter,
      this.listParameters,
    );
  }
}
