/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  afterNextRender,
  Component,
  computed,
  effect,
  EventEmitter,
  inject,
  Injector,
  OnDestroy,
  OnInit,
  signal,
  viewChild,
} from "@angular/core";
import { MatIconAnchor, MatIconButton } from "@angular/material/button";
import { MatDialog } from "@angular/material/dialog";
import { MatIcon } from "@angular/material/icon";
import { MatPaginator, PageEvent } from "@angular/material/paginator";
import {
  MatDrawer,
  MatDrawerContainer,
  MatDrawerContent,
} from "@angular/material/sidenav";
import {
  MatSort,
  MatSortHeader,
  Sort,
  SortDirection,
} from "@angular/material/sort";
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
} from "@angular/material/table";
import { MatTooltip } from "@angular/material/tooltip";
import { ActivatedRoute, RouterLink } from "@angular/router";
import { TranslatePipe } from "@ngx-translate/core";
import { GebruikersvoorkeurenService } from "../../gebruikersvoorkeuren/gebruikersvoorkeuren.service";
import { ZoekFilters } from "../../gebruikersvoorkeuren/zoekopdracht/zoekfilters.model";
import { ZoekopdrachtComponent } from "../../gebruikersvoorkeuren/zoekopdracht/zoekopdracht.component";
import { InformatieObjectLinkComponent } from "../../informatie-objecten/informatie-object-link/informatie-object-link.component";
import { InformatieObjectenService } from "../../informatie-objecten/informatie-objecten.service";
import {
  ConfirmDialogComponent,
  ConfirmDialogData,
} from "../../shared/confirm-dialog/confirm-dialog.component";
import { TypedMatCellDefDirective } from "../../shared/directives/typed-mat-cell-def.directive";
import { WerklijstComponent } from "../../shared/dynamic-table/datasource/werklijst-component";
import { PutBody } from "../../shared/http/http-client";
import { injectMutation } from "../../shared/http/inject-mutation";
import { injectLastSuccessfulSearch } from "../../shared/http/last-successful-search";
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

type InboxDocumentSearch = Omit<PutBody<"/rest/inboxdocumenten">, "order"> & {
  order?: SortDirection;
  filtersType: ZoekFilters["filtersType"];
};

@Component({
  templateUrl: "./inbox-documenten-list.component.html",
  styleUrls: ["./inbox-documenten-list.component.less"],
  standalone: true,
  imports: [
    RouterLink,
    MatDrawerContainer,
    MatDrawer,
    MatDrawerContent,
    MatTable,
    MatColumnDef,
    MatHeaderCellDef,
    MatHeaderCell,
    MatCellDef,
    TypedMatCellDefDirective,
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
    MatTooltip,
    TranslatePipe,
    TekstFilterComponent,
    DateRangeFilterComponent,
    ReadMoreComponent,
    ZoekopdrachtComponent,
    DatumPipe,
    InformatieObjectLinkComponent,
  ],
})
export class InboxDocumentenListComponent
  extends WerklijstComponent
  implements OnInit, OnDestroy
{
  private readonly inboxDocumentenService = inject(InboxDocumentenService);
  private readonly infoService = inject(InformatieObjectenService);
  private readonly dialog = inject(MatDialog);
  private readonly injector = inject(Injector);
  readonly gebruikersvoorkeurenService = inject(GebruikersvoorkeurenService);
  readonly route = inject(ActivatedRoute);

  /** `null` until the page size of the werklijst is known. */
  private readonly requestedSearch = signal<InboxDocumentSearch | null>(null);
  private readonly search = injectLastSuccessfulSearch({
    requested: this.requestedSearch,
    query: (search) => ({
      ...this.inboxDocumentenService.list(search ?? {}),
      enabled: search !== null,
    }),
  });
  protected readonly listParameters = computed(
    () => this.search.requested() ?? this.createDefaultParameters(),
  );
  protected readonly isLoadingResults = computed(
    () => this.search.requested() === null || this.search.query.isFetching(),
  );
  protected readonly totaal = computed(() => this.search.data()?.totaal ?? 0);
  protected readonly documenten = computed(
    () => this.search.data()?.resultaten ?? [],
  );
  readonly actionsSidenav = viewChild.required<MatDrawer>("actionsSidenav");

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
  protected clearZoekopdracht = new EventEmitter<void>();
  protected readonly selectedInformationObject =
    signal<GeneratedType<"RestInboxDocument"> | null>(null);
  private readonly deleteMutation = injectMutation(
    () => this.inboxDocumentenService.delete(),
    {
      onSuccess: () => this.retriggerSearch(),
    },
  );

  constructor() {
    super();
    effect(() => {
      const search = this.search.requested();
      if (search) {
        SessionStorageUtil.setItem(
          "INBOX_DOCUMENTEN_ZOEKPARAMETERS" satisfies WerklijstZoekParameter,
          search,
        );
      }
    });
  }

  ngOnInit() {
    super.ngOnInit();
    this.search.request({
      ...SessionStorageUtil.getItem<InboxDocumentSearch>(
        "INBOX_DOCUMENTEN_ZOEKPARAMETERS" satisfies WerklijstZoekParameter,
        this.createDefaultParameters(),
      ),
      page: 0,
      maxResults: this.aantalPerPagina(),
    });
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
        data: new ConfirmDialogData({
          key: "msg.document.verwijderen.bevestigen",
          args: { document: inboxDocument.titel },
        }),
      })
      .afterClosed()
      .subscribe((result) => {
        if (result) {
          this.deleteMutation.mutate(inboxDocument);
        }
      });
  }

  protected filtersChanged(changes: Partial<InboxDocumentSearch>) {
    this.searchFirstPage(changes);
    // The zoekopdracht reads its zoekFilters input when it is told, so only once that input has the new filters.
    afterNextRender(() => this.clearZoekopdracht.emit(), {
      injector: this.injector,
    });
  }

  protected sortChanged({ active, direction }: Sort) {
    this.searchFirstPage({
      sort: active as InboxDocumentSearch["sort"],
      order: direction,
    });
  }

  protected pageChanged(event: PageEvent) {
    this.paginatorChanged(event);
    this.search.request({
      ...this.listParameters(),
      page: event.pageIndex,
      maxResults: event.pageSize,
    });
  }

  resetSearch() {
    this.searchFirstPage(this.createDefaultParameters(), true);
  }

  retriggerSearch() {
    this.search.request(this.listParameters());
  }

  createDefaultParameters(): InboxDocumentSearch {
    return {
      sort: "creatiedatum",
      order: "desc",
      filtersType: "InboxDocumentListParameters",
    };
  }

  zoekopdrachtChanged(
    actieveZoekopdracht: GeneratedType<"RestZoekopdracht"> | null,
  ) {
    if (actieveZoekopdracht?.json) {
      this.searchFirstPage(
        {
          ...this.createDefaultParameters(),
          ...JSON.parse(actieveZoekopdracht.json),
        },
        true,
      );
    } else if (actieveZoekopdracht === null) {
      this.resetSearch();
    } else {
      this.retriggerSearch();
    }
  }

  private searchFirstPage(
    changes: Partial<InboxDocumentSearch>,
    replace = false,
  ) {
    const { maxResults } = this.listParameters();
    this.search.request({
      ...(replace ? this.createDefaultParameters() : this.listParameters()),
      ...changes,
      page: 0,
      maxResults,
    });
  }

  getWerklijst(): GeneratedType<"Werklijst"> {
    return "INBOX_DOCUMENTEN";
  }

  openDrawer(selectedInformationObject: GeneratedType<"RestInboxDocument">) {
    this.selectedInformationObject.set(selectedInformationObject);
    void this.actionsSidenav().open();
  }

  ngOnDestroy() {
    SessionStorageUtil.setItem(
      "INBOX_DOCUMENTEN_ZOEKPARAMETERS" satisfies WerklijstZoekParameter,
      { ...this.listParameters(), page: 0 },
    );
  }
}
