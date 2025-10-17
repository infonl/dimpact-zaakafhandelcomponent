/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, computed, effect, OnDestroy, signal } from "@angular/core";
import { injectQuery } from "@tanstack/angular-query-experimental";
import { firstValueFrom } from "rxjs";
import { WebsocketService } from "../../core/websocket/websocket.service";
import { IdentityService } from "../../identity/identity.service";
import { GeneratedType } from "../../shared/utils/generated-types";
import { SignaleringenService } from "../../signaleringen.service";
import { DashboardCardComponent } from "../dashboard-card/dashboard-card.component";

@Component({
  selector: "zac-zaken-card",
  templateUrl: "./zaken-card.component.html",
  styleUrls: [
    "../dashboard-card/dashboard-card.component.less",
    "./zaken-card.component.less",
  ],
})
export class ZakenCardComponent
  extends DashboardCardComponent<GeneratedType<"RestZaakOverzicht">>
  implements OnDestroy
{
  columns = [
    "identificatie",
    "startdatum",
    "zaaktype",
    "omschrijving",
    "url",
  ] as const;
  pageSize = 5;
  pageNumber = signal(0);

  parameters = computed(() => ({
    signaleringType: this.data.signaleringType,
    page: this.pageNumber(),
    pageSize: this.pageSize,
  }));

  zakenQuery = injectQuery(() => ({
    queryKey: ["aan mij toegekende zaken signaleringen", this.parameters()],
    queryFn: () =>
      firstValueFrom(
        this.signaleringenService.listZakenSignalering(
          this.parameters().signaleringType!,
          {
            page: this.parameters().page,
            rows: this.parameters().pageSize,
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
      const { resultaten = [], totaal = 0 } = this.zakenQuery.data() ?? {};

      this.dataSource.data = resultaten;
      if (this.paginator) this.paginator.length = totaal;
    });
  }

  onPageChange({ pageIndex }: { pageIndex: number }) {
    this.pageNumber.set(pageIndex);
  }

  protected onLoad(): void {
    this.zakenQuery.refetch();
  }
}
