/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  Component,
  computed,
  DestroyRef,
  effect,
  inject,
  OnDestroy,
  signal,
} from "@angular/core";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { MatIconAnchor } from "@angular/material/button";
import { MatIcon } from "@angular/material/icon";
import { MatPaginator } from "@angular/material/paginator";
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
import { injectQuery } from "@tanstack/angular-query-experimental";
import { DatumPipe } from "../../shared/pipes/datum.pipe";
import { EmptyPipe } from "../../shared/pipes/empty.pipe";
import { RefetchErrorHintComponent } from "../../shared/refetch-error-hint/refetch-error-hint.component";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZaakIdentificatieComponent } from "../../shared/zaak-identificatie/zaak-identificatie.component";
import { SignaleringenService } from "../../signaleringen.service";
import { DashboardCardComponent } from "../dashboard-card/dashboard-card.component";

const DEFAULT_SORT_FIELD: GeneratedType<"SorteerVeld"> = "SIGNALERING_TIJDSTIP";
const DEFAULT_SORT_ORDER: GeneratedType<"SorteerRichting"> = "DESC";

@Component({
  selector: "zac-zaken-card",
  templateUrl: "./zaken-card.component.html",
  styleUrls: [
    "../dashboard-card/dashboard-card.component.less",
    "./zaken-card.component.less",
  ],
  standalone: true,
  imports: [
    RefetchErrorHintComponent,
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
    MatProgressSpinner,
    MatIconAnchor,
    MatIcon,
    RouterLink,
    TranslatePipe,
    DatumPipe,
    EmptyPipe,
  ],
})
export class ZakenCardComponent
  extends DashboardCardComponent<GeneratedType<"RestZaakOverzicht">>
  implements OnDestroy
{
  private readonly signaleringenService = inject(SignaleringenService);

  private readonly destroyRef = inject(DestroyRef);

  columns = [
    "identificatie",
    "startdatum",
    "zaaktype",
    "omschrijving",
    "url",
  ] as const;
  protected override serverSidePagination = true;
  pageSize = 5;
  pageNumber = signal(0);
  sortField = signal<GeneratedType<"SorteerVeld">>(DEFAULT_SORT_FIELD);
  sortOrder = signal<GeneratedType<"SorteerRichting">>(DEFAULT_SORT_ORDER);

  parameters = computed(() => ({
    signaleringType: this.data().signaleringType,
    page: this.pageNumber(),
    pageSize: this.pageSize,
    sortField: this.sortField(),
    sortOrder: this.sortOrder(),
  }));

  zakenQuery = injectQuery(() => ({
    ...this.signaleringenService.listZakenSignalering(
      this.parameters().signaleringType!,
      {
        page: this.parameters().page,
        rows: this.parameters().pageSize,
        sortField: this.parameters().sortField,
        sortOrder: this.parameters().sortOrder,
      },
    ),
    enabled: this.parameters().signaleringType != null,
  }));

  constructor() {
    super();

    effect(() => {
      this.dataSource.data = this.zakenQuery.data()?.resultaten ?? [];
    });

    // Listing the signaleringen deletes the stale ones, so the total can drop below the page this card shows.
    // Asking for that page again is rejected by the backend, so move to the last page that still exists.
    // The cached pages are marked stale first, because they were loaded before the stale signaleringen
    // were deleted.
    effect(() => {
      const totaal = this.zakenQuery.data()?.totaal;
      if (totaal == null || this.zakenQuery.isFetching()) return;
      const lastPageNumber = Math.max(0, Math.ceil(totaal / this.pageSize) - 1);
      if (this.pageNumber() > lastPageNumber) {
        void this.signaleringenService.markZakenSignaleringStale();
        this.pageNumber.set(lastPageNumber);
      }
    });
  }

  override ngAfterViewInit(): void {
    super.ngAfterViewInit();
    const sort = this.sort();
    if (!sort) return;

    sort.sortChange
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(({ active, direction }: Sort) => {
        this.pageNumber.set(0);
        if (direction) {
          this.sortField.set(active as GeneratedType<"SorteerVeld">);
          this.sortOrder.set(
            direction.toUpperCase() as GeneratedType<"SorteerRichting">,
          );
        } else {
          this.sortField.set(DEFAULT_SORT_FIELD);
          this.sortOrder.set(DEFAULT_SORT_ORDER);
        }
      });
  }

  onPageChange({ pageIndex }: { pageIndex: number }) {
    this.pageNumber.set(pageIndex);
  }

  protected onLoad(): void {
    this.zakenQuery.refetch();
  }
}
