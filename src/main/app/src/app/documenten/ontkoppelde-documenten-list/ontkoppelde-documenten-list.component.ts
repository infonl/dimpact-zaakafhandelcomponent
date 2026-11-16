/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { SelectionModel } from "@angular/cdk/collections";
import {
  AfterViewInit,
  Component,
  computed,
  effect,
  EventEmitter,
  inject,
  OnDestroy,
  OnInit,
  signal,
  untracked,
  viewChild,
} from "@angular/core";
import { toSignal } from "@angular/core/rxjs-interop";
import { MatBadge } from "@angular/material/badge";
import {
  MatButton,
  MatIconAnchor,
  MatIconButton,
} from "@angular/material/button";
import { MatCheckbox } from "@angular/material/checkbox";
import { MatChip, MatChipSet } from "@angular/material/chips";
import { MatDialog } from "@angular/material/dialog";
import { MatFormField } from "@angular/material/form-field";
import { MatIcon } from "@angular/material/icon";
import { MatPaginator } from "@angular/material/paginator";
import { MatOption, MatSelect } from "@angular/material/select";
import {
  MatDrawer,
  MatDrawerContainer,
  MatDrawerContent,
} from "@angular/material/sidenav";
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
import { MatTooltip } from "@angular/material/tooltip";
import { ActivatedRoute, RouterLink } from "@angular/router";
import { TranslatePipe } from "@ngx-translate/core";
import { injectIsMutating } from "@tanstack/angular-query-experimental";
import { merge } from "rxjs";
import { map, startWith } from "rxjs/operators";
import { UtilService } from "../../core/service/util.service";
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
import { ZaakIdentificatieComponent } from "../../shared/zaak-identificatie/zaak-identificatie.component";
import { OntkoppeldeDocumentenService } from "../ontkoppelde-documenten.service";

type DetachedDocumentSearch = PutBody<"/rest/ontkoppeldedocumenten"> & {
  filtersType: ZoekFilters["filtersType"];
};

@Component({
  templateUrl: "./ontkoppelde-documenten-list.component.html",
  styleUrls: ["./ontkoppelde-documenten-list.component.less"],
  standalone: true,
  imports: [
    ZaakIdentificatieComponent,
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
    MatButton,
    MatIconButton,
    MatIconAnchor,
    MatCheckbox,
    MatBadge,
    MatChip,
    MatChipSet,
    MatIcon,
    MatTooltip,
    MatFormField,
    MatSelect,
    MatOption,
    TranslatePipe,
    TekstFilterComponent,
    DateRangeFilterComponent,
    ReadMoreComponent,
    ZoekopdrachtComponent,
    DatumPipe,
    InformatieObjectLinkComponent,
  ],
})
export class OntkoppeldeDocumentenListComponent
  extends WerklijstComponent
  implements OnInit, AfterViewInit, OnDestroy
{
  private readonly ontkoppeldeDocumentenService = inject(
    OntkoppeldeDocumentenService,
  );
  private readonly infoService = inject(InformatieObjectenService);
  private readonly utilService = inject(UtilService);
  private readonly dialog = inject(MatDialog);
  readonly gebruikersvoorkeurenService = inject(GebruikersvoorkeurenService);
  readonly route = inject(ActivatedRoute);

  /** `null` until the view exists, because the paginator decides the first page. */
  private readonly requestedSearch = signal<DetachedDocumentSearch | null>(
    null,
  );
  private readonly search = injectLastSuccessfulSearch({
    requested: this.requestedSearch,
    query: (search) => ({
      ...this.ontkoppeldeDocumentenService.list(search ?? {}),
      enabled: search !== null,
    }),
    onRevert: (search) => {
      if (search) this.revert(search);
    },
  });
  protected readonly isLoadingResults = computed(
    () => this.requestedSearch() === null || this.search.query.isFetching(),
  );
  private readonly processedIds = signal<ReadonlySet<number>>(new Set());
  private readonly shownList = computed(() => {
    const result = this.search.data();
    if (!result) return null;
    const resultaten = (result.resultaten ??
      []) as GeneratedType<"RestDetachedDocument">[];
    const documents = resultaten.filter(
      ({ id }) => !this.processedIds().has(id!),
    );
    return {
      documents,
      totaal: (result.totaal ?? 0) - (resultaten.length - documents.length),
      filterOntkoppeldDoor: result.filterOntkoppeldDoor ?? [],
    };
  });
  protected readonly totaal = computed(() => this.shownList()?.totaal ?? 0);
  protected dataSource = new MatTableDataSource<
    GeneratedType<"RestDetachedDocument">
  >();
  readonly paginator = viewChild.required(MatPaginator);
  readonly sort = viewChild.required(MatSort);
  readonly actionsSidenav = viewChild.required<MatDrawer>("actionsSidenav");

  private readonly dataColumns = [
    "titel",
    "creatiedatum",
    "zaakID",
    "ontkoppeldDoor",
    "ontkoppeldOp",
    "reden",
    "actions",
  ] as const;
  private readonly dataFilterColumns = [
    "titel_filter",
    "creatiedatum_filter",
    "zaakID_filter",
    "ontkoppeldDoor_filter",
    "ontkoppeldOp_filter",
    "reden_filter",
    "actions_filter",
  ] as const;
  protected get displayedColumns() {
    return this.canSelect ? ["select", ...this.dataColumns] : this.dataColumns;
  }
  protected get filterColumns() {
    return this.canSelect
      ? ["select_filter", ...this.dataFilterColumns]
      : this.dataFilterColumns;
  }
  protected listParameters: DetachedDocumentSearch =
    this.createDefaultParameters();
  protected readonly listParametersSort: {
    sort: keyof PutBody<"/rest/ontkoppeldedocumenten">;
    order: "desc" | "asc";
    filtersType: ZoekFilters["filtersType"];
  } = {
    sort: "ontkoppeldOp",
    order: "desc",
    filtersType: "DetachedDocumentListParameters",
  };
  protected readonly filterOntkoppeldDoor = computed(
    () => this.shownList()?.filterOntkoppeldDoor ?? [],
  );
  protected filterChange = new EventEmitter<void>();
  protected clearZoekopdracht = new EventEmitter<void>();
  protected readonly selectedInformationObject =
    signal<GeneratedType<"RestDetachedDocument"> | null>(null);
  protected readonly selection = new SelectionModel<
    GeneratedType<"RestDetachedDocument">
  >(true, []);
  private readonly selectedDetachedDocuments = toSignal(
    this.selection.changed.pipe(map(({ source }) => source.selected)),
    { initialValue: [] },
  );
  protected readonly isSelected = computed(
    () => this.selectedDetachedDocuments().length > 0,
  );
  protected readonly selectedCount = computed(
    () => this.selectedDetachedDocuments().length,
  );
  protected readonly selectedDocumentsToLink = signal<
    GeneratedType<"RestDetachedDocument">[]
  >([]);
  private readonly idsToKeepSelected = signal<ReadonlySet<number>>(new Set());
  private readonly isLoadPending = signal(false);
  private readonly documentsBeingDeleted = signal<
    GeneratedType<"RestDetachedDocument">[]
  >([]);
  private readonly deleteMutation = injectMutation(
    () => this.ontkoppeldeDocumentenService.delete(),
    {
      onSuccess: () => this.filterChange.emit(),
    },
  );
  // Reports from the options of the mutation, because those also run after the employee left the page.
  private readonly deleteOntkoppeldeDocumentenMutation = injectMutation(
    () => this.ontkoppeldeDocumentenService.deleteOntkoppeldeDocumenten(),
    {
      onSuccess: (result) =>
        this.ontkoppeldeDocumentenService.reportBulkResult(
          result,
          "verwijderen",
          this.documentsBeingDeleted(),
        ),
    },
  );
  private readonly runningBulkActions = injectIsMutating({
    predicate: ({ options }) =>
      String(options.mutationKey?.[0]).startsWith(
        "/rest/ontkoppeldedocumenten/lijst/",
      ),
  });
  protected readonly isBulkActionRunning = computed(
    () => this.runningBulkActions() > 0,
  );

  protected get canSelect() {
    return !!this.werklijstRechten()?.canOntkoppeldeDocumentenVerwijderen;
  }

  constructor() {
    super();
    effect(() => {
      const shownList = this.shownList();
      if (!shownList) return;
      untracked(() => this.showDocuments(shownList));
    });
    effect(() => {
      const isLoading = this.isLoadingResults();
      this.shownList();
      untracked(() => {
        this.utilService.setLoading(isLoading);
        if (!isLoading) this.finishLoad();
      });
    });
  }

  ngOnInit() {
    super.ngOnInit();
    this.listParameters = SessionStorageUtil.getItem(
      "ONTKOPPELDE_DOCUMENTEN_ZOEKPARAMETERS" satisfies WerklijstZoekParameter,
      this.createDefaultParameters(),
    );
  }

  ngAfterViewInit() {
    this.sort().sortChange.subscribe(() => (this.paginator().pageIndex = 0));
    merge(this.sort().sortChange, this.paginator().page, this.filterChange)
      .pipe(startWith({}))
      .subscribe(() => this.load());
  }

  private load() {
    this.selection.clear();
    this.isLoadPending.set(true);
    this.updateListParameters();
    this.search.request({
      ...this.listParametersSort,
      ...this.listParameters,
    });
  }

  private showDocuments({
    documents,
    totaal,
  }: {
    documents: GeneratedType<"RestDetachedDocument">[];
    totaal: number;
  }) {
    this.paginator().length = totaal;
    this.dataSource.data = documents;
    if (this.isLoadPending()) return;

    const selectedIds = new Set(this.selection.selected.map(({ id }) => id));
    this.selection.setSelection(
      ...documents.filter(({ id }) => selectedIds.has(id)),
    );
  }

  private finishLoad() {
    if (!this.isLoadPending()) return;
    this.isLoadPending.set(false);

    const { pageIndex, pageSize } = this.paginator();
    const lastPageIndex = Math.max(Math.ceil(this.totaal() / pageSize) - 1, 0);
    if (pageSize > 0 && pageIndex > lastPageIndex) {
      this.paginator().pageIndex = lastPageIndex;
      this.load();
      return;
    }
    const idsToKeepSelected = this.idsToKeepSelected();
    this.selection.setSelection(
      ...this.dataSource.data.filter(({ id }) => idsToKeepSelected.has(id!)),
    );
    this.idsToKeepSelected.set(new Set());
  }

  private revert(search: DetachedDocumentSearch) {
    this.listParameters = SessionStorageUtil.setItem(
      "ONTKOPPELDE_DOCUMENTEN_ZOEKPARAMETERS" satisfies WerklijstZoekParameter,
      search,
    );
    this.sort().active = search.sort ?? this.listParametersSort.sort;
    this.sort().direction = (search.order ?? this.listParametersSort.order) as
      | "asc"
      | "desc";
    this.paginator().pageIndex = search.page ?? 0;
    this.paginator().pageSize = search.maxResults ?? this.paginator().pageSize;
  }

  protected openDrawer(
    selectedInformationObject: GeneratedType<"RestDetachedDocument">,
  ) {
    this.selectedDocumentsToLink.set([]);
    this.selectedInformationObject.set(selectedInformationObject);
    void this.actionsSidenav().open();
  }

  protected updateListParameters() {
    this.listParameters.sort = this.sort().active;
    this.listParameters.order = this.sort().direction;
    this.listParameters.page = this.paginator().pageIndex;
    this.listParameters.maxResults = this.paginator().pageSize;
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
        data: new ConfirmDialogData({
          key: "msg.document.verwijderen.bevestigen",
          args: { document: detachedDocument.titel },
        }),
      })
      .afterClosed()
      .subscribe((result) => {
        if (result) {
          this.deleteMutation.mutate(detachedDocument);
        }
      });
  }

  protected isRowSelected(row: GeneratedType<"RestDetachedDocument">) {
    return this.selectedDetachedDocuments().includes(row);
  }

  protected isAllSelected() {
    return (
      this.dataSource.data.length > 0 &&
      this.selectedCount() === this.dataSource.data.length
    );
  }

  protected masterToggle() {
    if (this.isAllSelected()) {
      this.selection.clear();
      return;
    }

    this.selection.select(...this.dataSource.data);
  }

  protected checkboxLabel(
    detachedDocument?: GeneratedType<"RestDetachedDocument">,
  ) {
    if (!detachedDocument) {
      return `actie.alles.${this.isAllSelected() ? "deselecteren" : "selecteren"}`;
    }

    return `actie.${this.isRowSelected(detachedDocument) ? "deselecteren" : "selecteren"}`;
  }

  protected documentenVerwijderen() {
    const selectedDocuments = [...this.selection.selected];
    const ids = selectedDocuments.map(({ id }) => id!);
    this.documentsBeingDeleted.set(selectedDocuments);
    const enkelvoud = ids.length === 1 ? ".enkelvoud" : "";
    this.dialog
      .open(ConfirmDialogComponent, {
        data: new ConfirmDialogData(
          {
            key: `msg.ontkoppelde-documenten.bulk.verwijderen.bevestigen${enkelvoud}`,
            args: { aantal: ids.length },
          },
          undefined,
          undefined,
          {
            titel: {
              key: `msg.ontkoppelde-documenten.bulk.verwijderen.titel${enkelvoud}`,
              args: { aantal: ids.length },
            },
            icon: "delete",
            bevestigLabel: "actie.verwijderen",
            annuleerLabel: "actie.annuleren",
          },
        ),
      })
      .afterClosed()
      .subscribe((confirmed) => {
        if (!confirmed) return;

        this.deleteOntkoppeldeDocumentenMutation.mutate(
          { ids },
          {
            onSuccess: (result) =>
              this.reloadKeepingSelected(
                result.mislukt?.map(({ id }) => id!) ?? [],
                result.verwerkt ?? [],
              ),
            onError: () => this.reloadKeepingSelected(ids, []),
          },
        );
      });
  }

  protected documentenKoppelen() {
    const selectedDocuments = [...this.selection.selected];
    this.selectedDocumentsToLink.set(selectedDocuments);
    this.selectedInformationObject.set(selectedDocuments[0]);
    void this.actionsSidenav().open();
  }

  protected filtersChanged() {
    this.paginator().pageIndex = 0;
    this.clearZoekopdracht.emit();
    this.filterChange.emit();
  }

  protected resetSearch() {
    this.listParameters = SessionStorageUtil.setItem(
      "ONTKOPPELDE_DOCUMENTEN_ZOEKPARAMETERS" satisfies WerklijstZoekParameter,
      this.createDefaultParameters(),
    );
    this.sort().active = this.listParametersSort.sort;
    this.sort().direction = this.listParametersSort.order;
    this.paginator().pageIndex = 0;
    this.filterChange.emit();
  }

  protected zoekopdrachtChanged(
    actieveZoekopdracht: GeneratedType<"RestZoekopdracht"> | null,
  ) {
    if (actieveZoekopdracht) {
      this.listParameters = {
        ...this.createDefaultParameters(),
        ...JSON.parse(actieveZoekopdracht.json || "{}"),
      };
      this.sort().active = this.listParameters.sort!;
      this.sort().direction = this.listParameters.order as SortDirection;
      this.paginator().pageIndex = 0;
      this.filterChange.emit();
    } else if (actieveZoekopdracht === null) {
      this.resetSearch();
    } else {
      this.filterChange.emit();
    }
  }

  protected linkFinished(notLinkedIds: number[]) {
    this.reloadKeepingSelected(
      notLinkedIds,
      this.selectedDocumentsToLink()
        .map(({ id }) => id!)
        .filter((id) => !notLinkedIds.includes(id)),
    );
  }

  /**
   * Leaves out what was processed before it reloads, so that the table is right
   * when the reload fails too because Open Zaak is down.
   */
  private reloadKeepingSelected(idsToKeep: number[], idsProcessed: number[]) {
    this.processedIds.update((ids) => new Set([...ids, ...idsProcessed]));
    this.idsToKeepSelected.set(new Set(idsToKeep));
    this.filterChange.emit();
  }

  protected retriggerSearch() {
    this.filterChange.emit();
  }

  protected createDefaultParameters() {
    return {
      sort: "ontkoppeldOp",
      order: "desc",
      filtersType: "DetachedDocumentListParameters",
    } satisfies DetachedDocumentSearch;
  }

  protected readonly compareUser = (
    user1: GeneratedType<"RestUser">,
    user2: GeneratedType<"RestUser">,
  ) => {
    return user1?.username === user2?.username;
  };

  getWerklijst(): GeneratedType<"Werklijst"> {
    return "ONTKOPPELDE_DOCUMENTEN";
  }

  ngOnDestroy() {
    this.utilService.setLoading(false);
    // Make sure when returning to this component, the very first page is loaded
    this.listParameters.page = 0;
    SessionStorageUtil.setItem(
      "ONTKOPPELDE_DOCUMENTEN_ZOEKPARAMETERS" satisfies WerklijstZoekParameter,
      this.listParameters,
    );
  }
}
