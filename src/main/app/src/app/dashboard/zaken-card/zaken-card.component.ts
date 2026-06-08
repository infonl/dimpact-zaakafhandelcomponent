/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NgIf } from "@angular/common";
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
import { TranslateModule } from "@ngx-translate/core";
import { injectQuery } from "@tanstack/angular-query-experimental";
import { firstValueFrom } from "rxjs";
import { WebsocketService } from "../../core/websocket/websocket.service";
import { IdentityService } from "../../identity/identity.service";
import { DatumPipe } from "../../shared/pipes/datum.pipe";
import { EmptyPipe } from "../../shared/pipes/empty.pipe";
import { GeneratedType } from "../../shared/utils/generated-types";
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
    NgIf,
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
    TranslateModule,
    DatumPipe,
    EmptyPipe,
  ],
})
export class ZakenCardComponent
  extends DashboardCardComponent<GeneratedType<"RestZaakOverzicht">>
  implements OnDestroy
{
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
    signaleringType: this.data.signaleringType,
    page: this.pageNumber(),
    pageSize: this.pageSize,
    sortField: this.sortField(),
    sortOrder: this.sortOrder(),
  }));

  zakenQuery = injectQuery(() => ({
    queryKey: ["aan mij toegekende zaken signaleringen", this.parameters()],
    enabled: this.parameters().signaleringType != null,
    queryFn: () =>
      firstValueFrom(
        this.signaleringenService.listZakenSignalering(
          this.parameters().signaleringType!,
          {
            page: this.parameters().page,
            rows: this.parameters().pageSize,
            sortField: this.parameters().sortField,
            sortOrder: this.parameters().sortOrder,
          },
        ),
      ),
  }));

  constructor(
    private readonly signaleringenService: SignaleringenService,
    protected readonly identityService: IdentityService,
    protected readonly websocketService: WebsocketService,
  ) {
    super(identityService, websocketService);

    effect(() => {
      this.dataSource.data = this.zakenQuery.data()?.resultaten ?? [];
    });
  }

  override ngAfterViewInit(): void {
    super.ngAfterViewInit();
    if (!this.sort) return;

    this.sort.sortChange
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
