/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  ChangeDetectionStrategy,
  Component,
  computed,
  DestroyRef,
  effect,
  inject,
  signal,
  untracked,
} from "@angular/core";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { MatIconAnchor } from "@angular/material/button";
import { MatIcon } from "@angular/material/icon";
import { MatPaginator } from "@angular/material/paginator";
import { MatProgressBar } from "@angular/material/progress-bar";
import { MatProgressSpinner } from "@angular/material/progress-spinner";
import { MatSort, MatSortHeader, Sort } from "@angular/material/sort";
import {
  MatCell,
  MatCellDef,
  MatColumnDef,
  MatHeaderCell,
  MatHeaderCellDef,
  MatHeaderRow,
  MatHeaderRowDef,
  MatNoDataRow,
  MatRow,
  MatRowDef,
  MatTable,
} from "@angular/material/table";
import { RouterLink } from "@angular/router";
import { TranslatePipe } from "@ngx-translate/core";
import { hashKey } from "@tanstack/angular-query-experimental";
import { injectLastSuccessfulSearch } from "../../shared/http/last-successful-search";
import { DatumPipe } from "../../shared/pipes/datum.pipe";
import { EmptyPipe } from "../../shared/pipes/empty.pipe";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZaakIdentificatieComponent } from "../../shared/zaak-identificatie/zaak-identificatie.component";
import { ZakenMijnDatasource } from "../../zaken/zaken-mijn/zaken-mijn-datasource";
import {
  getDefaultZoekParameters,
  ZoekParameters,
} from "../../zoeken/model/zoek-parameters";
import { ZoekenService } from "../../zoeken/zoeken.service";
import { DashboardCardComponent } from "../dashboard-card/dashboard-card.component";

@Component({
  selector: "zac-zaak-zoeken-card",
  templateUrl: "./zaak-zoeken-card.component.html",
  styleUrls: [
    "../dashboard-card/dashboard-card.component.less",
    "./zaak-zoeken-card.component.less",
  ],
  standalone: true,
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [
    ZaakIdentificatieComponent,
    MatTable,
    MatSort,
    MatColumnDef,
    MatHeaderCellDef,
    MatHeaderCell,
    MatSortHeader,
    MatCellDef,
    MatCell,
    MatHeaderRowDef,
    MatHeaderRow,
    MatRowDef,
    MatRow,
    MatNoDataRow,
    MatPaginator,
    MatProgressBar,
    MatProgressSpinner,
    MatIconAnchor,
    MatIcon,
    RouterLink,
    TranslatePipe,
    DatumPipe,
    EmptyPipe,
  ],
})
export class ZaakZoekenCardComponent extends DashboardCardComponent {
  private readonly zoekenService = inject(ZoekenService);

  private readonly destroyRef = inject(DestroyRef);

  columns = [
    "identificatie",
    "startdatum",
    "zaaktypeOmschrijving",
    "omschrijving",
    "url",
  ] as const;
  protected override serverSidePagination = true;
  pageSize = 5;

  private readonly requestedZoekParameters = signal<ZoekParameters>({
    ...ZakenMijnDatasource.mijnLopendeZaken(getDefaultZoekParameters()),
    sorteerVeld: "ZAAK_STARTDATUM",
    sorteerRichting: "desc",
    rows: this.pageSize,
    page: 0,
  });

  protected readonly search = injectLastSuccessfulSearch({
    requested: this.requestedZoekParameters,
    query: (zoekParameters) => ({
      ...this.zoekenService.list(zoekParameters),
      refetchInterval: 60 * 1000,
    }),
  });

  protected readonly zoekParameters = this.search.requested;
  protected readonly totaal = computed(() => this.search.data()?.totaal ?? 0);

  protected readonly isLoadingAnotherPage = computed(
    () =>
      this.search.query.isFetching() &&
      this.search.shown() !== undefined &&
      hashKey([this.search.requested()]) !== hashKey([this.search.params()]),
  );

  constructor() {
    super();

    effect(() => {
      const resultaten = this.search.data()?.resultaten ?? [];
      untracked(() => {
        this.dataSource.data = resultaten as typeof this.dataSource.data;
      });
    });
  }

  override ngAfterViewInit(): void {
    super.ngAfterViewInit();
    const sort = this.sort();
    if (!sort) return;

    sort.sortChange
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(({ active, direction }: Sort) => {
        this.search.request({
          ...this.zoekParameters(),
          sorteerVeld: active as GeneratedType<"SorteerVeld">,
          sorteerRichting: direction,
          page: 0,
        });
      });
  }

  onPageChange({ pageIndex }: { pageIndex: number }) {
    this.search.request({ ...this.zoekParameters(), page: pageIndex });
  }

  protected onLoad(): void {
    this.search.request(this.zoekParameters());
  }
}
