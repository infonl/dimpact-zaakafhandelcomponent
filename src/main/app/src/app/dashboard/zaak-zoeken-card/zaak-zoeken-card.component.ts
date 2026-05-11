/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  Component,
  computed,
  DestroyRef,
  effect,
  inject,
  signal,
} from "@angular/core";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { Sort, SortDirection } from "@angular/material/sort";
import { injectQuery } from "@tanstack/angular-query-experimental";
import { firstValueFrom } from "rxjs";
import { WebsocketService } from "../../core/websocket/websocket.service";
import { IdentityService } from "../../identity/identity.service";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZakenMijnDatasource } from "../../zaken/zaken-mijn/zaken-mijn-datasource";
import { getDefaultZoekParameters } from "../../zoeken/model/zoek-parameters";
import { ZoekenService } from "../../zoeken/zoeken.service";
import { DashboardCardComponent } from "../dashboard-card/dashboard-card.component";

@Component({
  selector: "zac-zaak-zoeken-card",
  templateUrl: "./zaak-zoeken-card.component.html",
  styleUrls: [
    "../dashboard-card/dashboard-card.component.less",
    "./zaak-zoeken-card.component.less",
  ],
  standalone: false,
})
export class ZaakZoekenCardComponent extends DashboardCardComponent {
  private readonly destroyRef = inject(DestroyRef);

  columns = [
    "identificatie",
    "startdatum",
    "zaaktypeOmschrijving",
    "omschrijving",
    "url",
  ] as const;
  pageSize = 5;
  pageNumber = signal(0);
  sortField = signal<GeneratedType<"SorteerVeld">>("ZAAK_STARTDATUM");
  sortDirection = signal<SortDirection>("desc");

  zoekParameters = computed(() => {
    const zoekParameters = ZakenMijnDatasource.mijnLopendeZaken(
      getDefaultZoekParameters(),
    );
    zoekParameters.sorteerVeld = this.sortField();
    zoekParameters.sorteerRichting = this.sortDirection();
    zoekParameters.rows = this.pageSize;
    zoekParameters.page = this.pageNumber();
    return zoekParameters;
  });

  protected readonly zoekQuery = injectQuery(() => ({
    queryKey: ["zaak zoeken dashboard", this.zoekParameters()],
    queryFn: () =>
      firstValueFrom(this.zoekenService.list(this.zoekParameters())),
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
