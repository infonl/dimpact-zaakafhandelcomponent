/*
 * SPDX-FileCopyrightText: 2023 Atos, 2024 INFO.nl
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
import { MatSort, SortDirection } from "@angular/material/sort";
import { MatTableDataSource } from "@angular/material/table";
import { DomSanitizer, SafeUrl } from "@angular/platform-browser";
import { ActivatedRoute, Router } from "@angular/router";
import { merge } from "rxjs";
import { map, startWith, switchMap } from "rxjs/operators";
import { UtilService } from "../../core/service/util.service";
import { GebruikersvoorkeurenService } from "../../gebruikersvoorkeuren/gebruikersvoorkeuren.service";
import { InformatieObjectenService } from "../../informatie-objecten/informatie-objecten.service";
import { detailExpand } from "../../shared/animations/animations";
import {
  ConfirmDialogComponent,
  ConfirmDialogData,
} from "../../shared/confirm-dialog/confirm-dialog.component";
import { WerklijstComponent } from "../../shared/dynamic-table/datasource/werklijst-component";
import {
  SessionStorageUtil,
  WerklijstZoekParameter,
} from "../../shared/storage/session-storage.util";
import { GeneratedType } from "../../shared/utils/generated-types";
import { DatumRange } from "../../zoeken/model/datum-range";
import { InboxProductaanvragenService } from "../inbox-productaanvragen.service";

@Component({
  templateUrl: "./inbox-productaanvragen-list.component.html",
  styleUrls: ["./inbox-productaanvragen-list.component.less"],
  animations: [detailExpand],
})
export class InboxProductaanvragenListComponent
  extends WerklijstComponent
  implements OnInit, AfterViewInit, OnDestroy
{
  isLoadingResults = true;
  dataSource = new MatTableDataSource<
    GeneratedType<"RESTInboxProductaanvraag">
  >();
  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;
  displayedColumns = [
    "expand",
    "type",
    "ontvangstdatum",
    "initiator",
    "aantal_bijlagen",
    "actions",
  ] as const;
  filterColumns = [
    "expand_filter",
    "type_filter",
    "ontvangstdatum_filter",
    "initiator_filter",
    "aantal_bijlagen_filter",
    "actions_filter",
  ] as const;
  listParameters = SessionStorageUtil.getItem(
    `${this.getWerklijst()}_ZOEKPARAMETERS` satisfies WerklijstZoekParameter,
    this.createDefaultParameters(),
  );
  expandedRow: GeneratedType<"RESTInboxProductaanvraag"> | null = null;
  filterType: string[] = [];
  filterChange = new EventEmitter<void>();
  clearZoekopdracht = new EventEmitter<void>();
  previewSrc: SafeUrl | null = null;

  constructor(
    private readonly inboxProductaanvragenService: InboxProductaanvragenService,
    private readonly infoService: InformatieObjectenService,
    private readonly utilService: UtilService,
    public readonly dialog: MatDialog,
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

  updateListParameters() {
    this.listParameters.sort = this.sort.active;
    this.listParameters.order = this.sort.direction;
    this.listParameters.page = this.paginator.pageIndex;
    this.listParameters.maxResults = this.paginator.pageSize;
    SessionStorageUtil.setItem(
      `${this.getWerklijst()}_ZOEKPARAMETERS` satisfies WerklijstZoekParameter,
      this.listParameters,
    );
  }

  getDownloadURL(ip: GeneratedType<"RESTInboxProductaanvraag">) {
    return this.infoService.getDownloadURL(ip.aanvraagdocumentUUID!);
  }

  filtersChanged(options: {
    event: MatSelectChange | string | DatumRange;
    filter: keyof GeneratedType<"RESTInboxProductaanvraagListParameters">;
  }) {
    this.listParameters[options.filter] =
      typeof options.event === "object" && "value" in options.event
        ? (options.event.value as never)
        : (undefined as never);
    this.paginator.pageIndex = 0;
    this.clearZoekopdracht.emit();
    this.filterChange.emit();
  }

  resetSearch() {
    this.listParameters = SessionStorageUtil.setItem(
      `${this.getWerklijst()}_ZOEKPARAMETERS` satisfies WerklijstZoekParameter,
      this.createDefaultParameters(),
    );
    this.sort.active = this.listParameters.sort ?? "id";
    this.sort.direction = this.listParameters.order as SortDirection;
    this.paginator.pageIndex = 0;
    this.filterChange.emit();
  }

  zoekopdrachtChanged(actieveZoekopdracht: GeneratedType<"RESTZoekopdracht">) {
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

  createDefaultParameters(): GeneratedType<"RESTInboxProductaanvraagListParameters"> {
    return { sort: "id", order: "desc" };
  }

  getWerklijst(): GeneratedType<"Werklijst"> {
    return "INBOX_PRODUCTAANVRAGEN";
  }

  updateActive(selectedRow: GeneratedType<"RESTInboxProductaanvraag">) {
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

  aanmakenZaak(
    inboxProductaanvraag: GeneratedType<"RESTInboxProductaanvraag">,
  ) {
    this.router.navigateByUrl("zaken/create", {
      state: { inboxProductaanvraag },
    });
  }

  inboxProductaanvragenVerwijderen(
    inboxProductaanvraag: GeneratedType<"RESTInboxProductaanvraag">,
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
