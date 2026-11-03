/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { SelectionModel } from "@angular/cdk/collections";
import {
  AfterViewInit,
  Component,
  computed,
  EventEmitter,
  inject,
  OnDestroy,
  OnInit,
  signal,
  viewChild,
} from "@angular/core";
import { MatBadge } from "@angular/material/badge";
import {
  MatButton,
  MatIconAnchor,
  MatIconButton,
} from "@angular/material/button";
import { MatCheckbox } from "@angular/material/checkbox";
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
import { TranslatePipe } from "@ngx-translate/core";
import { injectIsMutating } from "@tanstack/angular-query-experimental";
import { merge } from "rxjs";
import { startWith, switchMap, tap } from "rxjs/operators";
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
import { WerklijstComponent } from "../../shared/dynamic-table/datasource/werklijst-component";
import { PutBody } from "../../shared/http/http-client";
import { injectMutation } from "../../shared/http/inject-mutation";
import { runQuery } from "../../shared/http/run-query";
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

const BULK_ACTION_URLS: unknown[] = [
  "/rest/ontkoppeldedocumenten/lijst/verwijderen",
  "/rest/ontkoppeldedocumenten/lijst/koppelen",
];

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
    MatIcon,
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

  protected readonly isLoadingResults = signal(true);
  protected readonly totaal = signal(0);
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
  protected readonly filterOntkoppeldDoor = signal<GeneratedType<"RestUser">[]>(
    [],
  );
  protected filterChange = new EventEmitter<void>();
  protected clearZoekopdracht = new EventEmitter<void>();
  protected readonly selectedInformationObject =
    signal<GeneratedType<"RestDetachedDocument"> | null>(null);
  protected readonly selection = new SelectionModel<
    GeneratedType<"RestDetachedDocument">
  >(true, []);
  protected selectedDocumentsToLink: GeneratedType<"RestDetachedDocument">[] =
    [];
  private idsToKeepSelected = new Set<number>();
  private readonly deleteMutation = injectMutation(
    () => this.ontkoppeldeDocumentenService.delete(),
    {
      onSuccess: () => this.filterChange.emit(),
    },
  );
  private readonly deleteListMutation = injectMutation(() =>
    this.ontkoppeldeDocumentenService.deleteList(),
  );
  private readonly runningBulkActions = injectIsMutating({
    predicate: (mutation) =>
      BULK_ACTION_URLS.includes(mutation.options.mutationKey?.[0]),
  });
  protected readonly isBulkActionRunning = computed(
    () => this.runningBulkActions() > 0,
  );

  protected get canSelect() {
    return !!this.werklijstRechten?.canOntkoppeldeDocumentenVerwijderen;
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
    const stopLoading = () => {
      this.isLoadingResults.set(false);
      this.utilService.setLoading(false);
    };
    merge(this.sort().sortChange, this.paginator().page, this.filterChange)
      .pipe(
        startWith({}),
        switchMap(() => {
          this.selection.clear();
          this.isLoadingResults.set(true);
          this.utilService.setLoading(true);
          this.updateListParameters();
          return runQuery(
            this.queryClient,
            this.ontkoppeldeDocumentenService.list({
              ...this.listParametersSort,
              ...this.listParameters,
            }),
          );
        }),
        tap({ next: stopLoading, error: stopLoading }),
      )
      .subscribe((data) => {
        const totaal = data.totaal ?? 0;
        const { pageIndex, pageSize } = this.paginator();
        const lastPageIndex = Math.max(Math.ceil(totaal / pageSize) - 1, 0);
        if (pageSize > 0 && pageIndex > lastPageIndex) {
          this.paginator().pageIndex = lastPageIndex;
          this.filterChange.emit();
          return;
        }
        this.paginator().length = totaal;
        this.totaal.set(totaal);
        this.filterOntkoppeldDoor.set(
          data.resultaten?.map(
            (result: GeneratedType<"RestDetachedDocument">) =>
              result.ontkoppeldDoor!,
          ) ?? [],
        );
        this.dataSource.data = data.resultaten ?? [];
        this.selection.setSelection(
          ...this.dataSource.data.filter(({ id }) =>
            this.idsToKeepSelected.has(id!),
          ),
        );
        this.idsToKeepSelected.clear();
      });
  }

  protected openDrawer(
    selectedInformationObject: GeneratedType<"RestDetachedDocument">,
  ) {
    this.selectedDocumentsToLink = [];
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

  protected isAllSelected() {
    return (
      this.dataSource.data.length > 0 &&
      this.selection.selected.length === this.dataSource.data.length
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

    return `actie.${
      this.selection.isSelected(detachedDocument)
        ? "deselecteren"
        : "selecteren"
    }`;
  }

  protected documentenVerwijderen() {
    const selectedDocuments = [...this.selection.selected];
    const ids = selectedDocuments.map(({ id }) => id!);
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

        this.deleteListMutation.mutate(
          { ids },
          {
            onSuccess: (result) => {
              this.ontkoppeldeDocumentenService.reportBulkResult(
                result,
                "verwijderen",
                selectedDocuments,
              );
              this.reloadKeepingSelected(
                result.mislukt?.map(({ id }) => id!) ?? [],
              );
            },
            onError: () => this.reloadKeepingSelected(ids),
          },
        );
      });
  }

  protected documentenKoppelen() {
    this.selectedDocumentsToLink = [...this.selection.selected];
    this.selectedInformationObject = this.selectedDocumentsToLink[0];
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
    actieveZoekopdracht: GeneratedType<"RestZoekopdracht">,
  ) {
    if (actieveZoekopdracht) {
      this.listParameters = JSON.parse(actieveZoekopdracht.json || "{}");
      this.sort().active = this.listParametersSort.sort;
      this.sort().direction = this.listParametersSort.order;
      this.paginator().pageIndex = 0;
      this.filterChange.emit();
    } else if (actieveZoekopdracht === null) {
      this.resetSearch();
    } else {
      this.filterChange.emit();
    }
  }

  protected reloadKeepingSelected(ids: number[]) {
    this.idsToKeepSelected = new Set(ids);
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
    } satisfies typeof this.listParametersSort;
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
    // Make sure when returning to this component, the very first page is loaded
    this.listParameters.page = 0;
    SessionStorageUtil.setItem(
      "ONTKOPPELDE_DOCUMENTEN_ZOEKPARAMETERS" satisfies WerklijstZoekParameter,
      this.listParameters,
    );
  }
}
