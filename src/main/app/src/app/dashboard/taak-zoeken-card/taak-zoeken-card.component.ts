/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NgIf } from "@angular/common";
import {
  Component,
  computed,
  DestroyRef,
  effect,
  inject,
  signal,
} from "@angular/core";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { MatIconAnchor } from "@angular/material/button";
import { MatIcon } from "@angular/material/icon";
import { MatPaginator } from "@angular/material/paginator";
import { MatProgressSpinner } from "@angular/material/progress-spinner";
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
import { TakenMijnDatasource } from "../../taken/taken-mijn/taken-mijn-datasource";
import { getDefaultZoekParameters } from "../../zoeken/model/zoek-parameters";
import { ZoekenService } from "../../zoeken/zoeken.service";
import { DashboardCardComponent } from "../dashboard-card/dashboard-card.component";

@Component({
  selector: "zac-taak-zoeken-card",
  templateUrl: "./taak-zoeken-card.component.html",
  styleUrls: [
    "../dashboard-card/dashboard-card.component.less",
    "./taak-zoeken-card.component.less",
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
export class TaakZoekenCardComponent extends DashboardCardComponent {
  private readonly destroyRef = inject(DestroyRef);

  columns = [
    "naam",
    "creatiedatum",
    "zaakIdentificatie",
    "zaaktypeOmschrijving",
    "url",
  ] as const;
  protected override serverSidePagination = true;
  pageSize = 5;
  pageNumber = signal(0);
  sortField = signal<GeneratedType<"SorteerVeld">>("TAAK_CREATIEDATUM");
  sortDirection = signal<SortDirection>("desc");

  zoekParameters = computed(() => {
    const zoekParameters = TakenMijnDatasource.mijnLopendeTaken(
      getDefaultZoekParameters(),
    );
    zoekParameters.sorteerVeld = this.sortField();
    zoekParameters.sorteerRichting = this.sortDirection();
    zoekParameters.rows = this.pageSize;
    zoekParameters.page = this.pageNumber();
    return zoekParameters;
  });

  protected readonly zoekQuery = injectQuery(() => ({
    queryKey: ["taak zoeken dashboard", this.zoekParameters()],
    queryFn: () =>
      firstValueFrom(this.zoekenService.list(this.zoekParameters())),
    refetchInterval: 60 * 1000,
  }));

  constructor(
    private zoekenService: ZoekenService,
    protected identityService: IdentityService,
    protected websocketService: WebsocketService,
  ) {
    super(identityService, websocketService);
    effect(() => {
      this.dataSource.data = this.zoekQuery.data()?.resultaten ?? [];
    });
  }

  override ngAfterViewInit(): void {
    super.ngAfterViewInit();
    if (!this.sort) return;

    this.sort.sortChange
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(({ active, direction }: Sort) => {
        this.pageNumber.set(0);
        this.sortField.set(active as GeneratedType<"SorteerVeld">);
        this.sortDirection.set(direction);
      });
  }

  onPageChange({ pageIndex }: { pageIndex: number }) {
    this.pageNumber.set(pageIndex);
  }

  protected onLoad(): void {
    this.zoekQuery.refetch();
  }
}
