/*
 * SPDX-FileCopyrightText: 2023 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  afterNextRender,
  Component,
  computed,
  DestroyRef,
  effect,
  EventEmitter,
  inject,
  Injector,
  OnDestroy,
  OnInit,
  signal,
} from "@angular/core";
import { MatIconAnchor, MatIconButton } from "@angular/material/button";
import { MatDialog } from "@angular/material/dialog";
import { MatFormField } from "@angular/material/form-field";
import { MatIcon } from "@angular/material/icon";
import { MatPaginator, PageEvent } from "@angular/material/paginator";
import { MatOption, MatSelect } from "@angular/material/select";
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
import { DomSanitizer, SafeUrl } from "@angular/platform-browser";
import { ActivatedRoute, Router, RouterLink } from "@angular/router";
import { TranslatePipe } from "@ngx-translate/core";
import { UtilService } from "../../core/service/util.service";
import { GebruikersvoorkeurenService } from "../../gebruikersvoorkeuren/gebruikersvoorkeuren.service";
import { ZoekopdrachtComponent } from "../../gebruikersvoorkeuren/zoekopdracht/zoekopdracht.component";
import { InformatieObjectenService } from "../../informatie-objecten/informatie-objecten.service";
import { detailExpand } from "../../shared/animations/animations";
import {
  ConfirmDialogComponent,
  ConfirmDialogData,
} from "../../shared/confirm-dialog/confirm-dialog.component";
import { TypedMatCellDefDirective } from "../../shared/directives/typed-mat-cell-def.directive";
import { WerklijstComponent } from "../../shared/dynamic-table/datasource/werklijst-component";
import { injectMutation } from "../../shared/http/inject-mutation";
import { injectLastSuccessfulSearch } from "../../shared/http/last-successful-search";
import { DatumPipe } from "../../shared/pipes/datum.pipe";
import {
  SessionStorageUtil,
  WerklijstZoekParameter,
} from "../../shared/storage/session-storage.util";
import { DateRangeFilterComponent } from "../../shared/table-zoek-filters/date-range-filter/date-range-filter.component";
import { TekstFilterComponent } from "../../shared/table-zoek-filters/tekst-filter/tekst-filter.component";
import { GeneratedType } from "../../shared/utils/generated-types";
import { InboxProductaanvragenService } from "../inbox-productaanvragen.service";

type InboxProductaanvraagListParameters =
  GeneratedType<"RestInboxProductaanvraagListParameters"> & {
    sort: string;
    order: SortDirection;
    filtersType: "InboxProductaanvraagListParameters";
  };

@Component({
  templateUrl: "./inbox-productaanvragen-list.component.html",
  styleUrls: ["./inbox-productaanvragen-list.component.less"],
  animations: [detailExpand],
  standalone: true,
  imports: [
    RouterLink,
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
    MatFormField,
    MatSelect,
    MatOption,
    TranslatePipe,
    TekstFilterComponent,
    DateRangeFilterComponent,
    ZoekopdrachtComponent,
    DatumPipe,
  ],
})
export class InboxProductaanvragenListComponent
  extends WerklijstComponent
  implements OnInit, OnDestroy
{
  private readonly inboxProductaanvragenService = inject(
    InboxProductaanvragenService,
  );
  private readonly infoService = inject(InformatieObjectenService);
  private readonly utilService = inject(UtilService);
  private readonly dialog = inject(MatDialog);
  private readonly injector = inject(Injector);
  readonly gebruikersvoorkeurenService = inject(GebruikersvoorkeurenService);
  readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly sanitizer = inject(DomSanitizer);

  /** `null` until the page size of the werklijst is known. */
  private readonly requestedSearch =
    signal<InboxProductaanvraagListParameters | null>(null);
  private readonly search = injectLastSuccessfulSearch({
    requested: this.requestedSearch,
    query: (search) => ({
      ...this.inboxProductaanvragenService.list(search ?? {}),
      enabled: search !== null,
    }),
  });
  protected readonly listParameters = computed(
    () => this.search.requested() ?? this.createDefaultParameters(),
  );
  protected readonly isLoadingResults = computed(
    () => this.search.requested() === null || this.search.query.isFetching(),
  );
  protected readonly totaal = computed(() =>
    Number(this.search.data()?.totaal ?? 0),
  );
  protected readonly productaanvragen = computed(
    () => this.search.data()?.resultaten ?? [],
  );
  protected readonly filterType = computed(
    () =>
      (this.search.data() as { filterType?: string[] } | undefined)
        ?.filterType ?? [],
  );
  protected readonly displayedColumns = [
    "expand",
    "type",
    "ontvangstdatum",
    "initiator",
    "aantal_bijlagen",
    "actions",
  ] as const;
  private readonly sortableColumns: string[] = [
    "id",
    "type",
    "ontvangstdatum",
    "initiatorID",
  ];
  protected readonly filterColumns = [
    "expand_filter",
    "type_filter",
    "ontvangstdatum_filter",
    "initiator_filter",
    "aantal_bijlagen_filter",
    "actions_filter",
  ] as const;
  protected readonly expandedRow =
    signal<GeneratedType<"RestInboxProductaanvraag"> | null>(null);
  protected clearZoekopdracht = new EventEmitter<void>();
  protected readonly previewSrc = signal<SafeUrl | null>(null);
  private readonly deleteMutation = injectMutation(
    () => this.inboxProductaanvragenService.delete(),
    {
      onSuccess: () => this.search.request(this.listParameters()),
    },
  );

  constructor() {
    super();
    effect(() => {
      const search = this.search.requested();
      if (search) {
        SessionStorageUtil.setItem(
          `${this.getWerklijst()}_ZOEKPARAMETERS` satisfies WerklijstZoekParameter,
          search,
        );
      }
    });
    effect(() => this.utilService.setLoading(this.isLoadingResults()));
    inject(DestroyRef).onDestroy(() => this.utilService.setLoading(false));
  }

  ngOnInit() {
    super.ngOnInit();
    this.search.request(
      this.withSupportedSort({
        ...SessionStorageUtil.getItem<InboxProductaanvraagListParameters>(
          `${this.getWerklijst()}_ZOEKPARAMETERS` satisfies WerklijstZoekParameter,
          this.createDefaultParameters(),
        ),
        page: 0,
        maxResults: this.aantalPerPagina(),
      }),
    );
  }

  protected getDownloadURL(ip: GeneratedType<"RestInboxProductaanvraag">) {
    return this.infoService.getDownloadURL(ip.aanvraagdocumentUUID!);
  }

  protected filtersChanged(
    changes: Partial<InboxProductaanvraagListParameters>,
  ) {
    this.searchFirstPage(changes);
    // The zoekopdracht reads its zoekFilters input when it is told, so only once that input has the new filters.
    afterNextRender(() => this.clearZoekopdracht.emit(), {
      injector: this.injector,
    });
  }

  protected sortChanged({ active, direction }: Sort) {
    this.searchFirstPage({ sort: active, order: direction });
  }

  protected pageChanged(event: PageEvent) {
    this.paginatorChanged(event);
    this.search.request({
      ...this.listParameters(),
      page: event.pageIndex,
      maxResults: event.pageSize,
    });
  }

  protected resetSearch() {
    this.searchFirstPage(this.createDefaultParameters(), true);
  }

  protected zoekopdrachtChanged(
    actieveZoekopdracht: GeneratedType<"RestZoekopdracht"> | null,
  ) {
    if (actieveZoekopdracht?.json) {
      this.searchFirstPage(
        this.withSupportedSort({
          ...this.createDefaultParameters(),
          ...JSON.parse(actieveZoekopdracht.json),
        }),
        true,
      );
    } else if (actieveZoekopdracht === null) {
      this.resetSearch();
    } else {
      this.search.request(this.listParameters());
    }
  }

  private searchFirstPage(
    changes: Partial<InboxProductaanvraagListParameters>,
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

  private withSupportedSort(
    listParameters: InboxProductaanvraagListParameters,
  ): InboxProductaanvraagListParameters {
    if (this.sortableColumns.includes(listParameters.sort)) {
      return listParameters;
    }
    const { sort, order } = this.createDefaultParameters();
    return { ...listParameters, sort, order };
  }

  protected createDefaultParameters(): InboxProductaanvraagListParameters {
    return {
      sort: "id",
      order: "desc",
      filtersType: "InboxProductaanvraagListParameters",
    };
  }

  getWerklijst(): GeneratedType<"Werklijst"> {
    return "INBOX_PRODUCTAANVRAGEN";
  }

  protected updateActive(
    selectedRow: GeneratedType<"RestInboxProductaanvraag">,
  ) {
    if (this.expandedRow() === selectedRow) {
      this.expandedRow.set(null);
      this.previewSrc.set(null);
    } else {
      this.expandedRow.set(selectedRow);
      this.previewSrc.set(
        this.sanitizer.bypassSecurityTrustResourceUrl(
          this.inboxProductaanvragenService.pdfPreview(
            selectedRow.aanvraagdocumentUUID!,
          ),
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
          "msg.inbox-productaanvraag.verwijderen.bevestigen",
        ),
      })
      .afterClosed()
      .subscribe((result) => {
        if (result) {
          this.deleteMutation.mutate(Number(inboxProductaanvraag.id ?? -1));
        }
      });
  }

  ngOnDestroy() {
    SessionStorageUtil.setItem(
      `${this.getWerklijst()}_ZOEKPARAMETERS` satisfies WerklijstZoekParameter,
      { ...this.listParameters(), page: 0 },
    );
  }
}
